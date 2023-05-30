package kafka

import cats.effect.Async
import datatypes.{DELETE_SUBSCRIPTION, EventMessage, KafkaMessage, NEW_SUBSCRIPTION}
import fs2.kafka._
import fs2.Stream
import cats.implicits._
import database.DatabaseService
import datatypes.KnownErrors.CommandNotImplemented
import github.GitHubService

import java.util.UUID

trait Streaming[F[_]] {
  def sendMessage(topic: String, message: EventMessage): F[Unit]
  def consumeMessages(topic: String): Stream[F, KafkaMessage]
}

object Streaming {

  def apply[F[_]](implicit ev: Streaming[F]): Streaming[F] = ev

  def impl[F[_]: Async](
      maxConcurrent: Int,
      producer: KafkaProducer[F, UUID, EventMessage],
      consumer: Stream[F, KafkaConsumer[F, String, KafkaMessage]],
      db: DatabaseService[F],
      g: GitHubService[F]
  ): Streaming[F] =
    new Streaming[F] {

      def sendMessage(topic: String, message: EventMessage): F[Unit] =
        producer.produce(
          ProducerRecords.one(ProducerRecord(topic, message.repoId, message))
        ) >> Async[F].unit

      def consumeMessages(topic: String): Stream[F, KafkaMessage] =
        consumer
          .evalTap(_.subscribeTo(topic))
          .flatMap(_.stream)
          .mapAsync(maxConcurrent)(committable => processRecord(committable.record))

      def processRecord(record: ConsumerRecord[String, KafkaMessage]): F[KafkaMessage] =
        for {
          numSubscriptions <-
            db.checkActiveSubscriptions(record.value.organization, record.value.repository)
          _ <- processKafkaMessage(record.value, numSubscriptions)
        } yield record.value

      def processKafkaMessage(message: KafkaMessage, numSubscriptions: Option[Int]): F[Unit] =
        (message.operationType, numSubscriptions) match {
          case (NEW_SUBSCRIPTION, None) =>
            for {
              webhookEvent <- g.createWebhook(message.organization, message.repository)
              _            <- db.addRepository(message.organization, message.repository, webhookEvent.id)
            } yield ()
          case (NEW_SUBSCRIPTION, Some(_)) =>
            for {
              _ <- db.incrementSubscription(message.organization, message.repository)
            } yield ()
          case (DELETE_SUBSCRIPTION, Some(1)) =>
            for {
              webhookId <- db.getWebhookId(message.organization, message.repository)
              _         <- db.removeRepository(message.organization, message.repository)
              _         <- g.removeWebhook(message.organization, message.repository, webhookId.get)
            } yield ()
          case (DELETE_SUBSCRIPTION, Some(_)) =>
            for {
              _ <- db.decrementSubscription(message.organization, message.repository)
            } yield ()
          case (DELETE_SUBSCRIPTION, None) =>
            Async[F].unit
          case _ =>
            Async[F].raiseError(CommandNotImplemented("Operation not implemented yet."))
        }
    }
}
