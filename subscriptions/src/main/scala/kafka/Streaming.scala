package kafka

import cats.effect.Async
import cats.implicits._
import database.DatabaseService
import datatypes._
import fs2.Stream
import fs2.kafka._

import java.util.UUID

trait Streaming[F[_]] {
  def sendMessage(topic: String, message: KafkaMessage): F[Unit]
  def consumeMessages(inputTopic: String, sinkTopic: String): Stream[F, EventMessage]
}

object Streaming {

  def apply[F[_]](implicit ev: Streaming[F]): Streaming[F] = ev

  def impl[F[_]: Async](
      maxConcurrent: Int,
      kafkaMessageProducer: KafkaProducer[F, String, KafkaMessage],
      notificationMessageProducer: KafkaProducer[F, String, NotificationMessage],
      consumer: Stream[F, KafkaConsumer[F, UUID, EventMessage]],
      db: DatabaseService[F]
  ): Streaming[F] =
    new Streaming[F] {
      def sendMessage(topic: String, message: KafkaMessage): F[Unit] =
        kafkaMessageProducer.produce(
          ProducerRecords.one(ProducerRecord(topic, message.userId, message))
        ) >> Async[F].unit

      def consumeMessages(inputTopic: String, sinkTopic: String): Stream[F, EventMessage] =
        consumer
          .evalTap(_.subscribeTo(inputTopic))
          .flatMap(_.stream)
          .mapAsync(maxConcurrent)(committable => processRecord(committable.record, sinkTopic))

      def processRecord(
          record: ConsumerRecord[UUID, EventMessage],
          topic: String
      ): F[EventMessage] =
        processEvent(record.key, record.value, topic)

      def processEvent(repoId: UUID, event: EventMessage, topic: String): F[EventMessage] =
        for {
          subscriptions <- db.getSubscribedUsers(repoId)
          _             <- subscriptions.traverse(sendNotification(_, event, topic))
        } yield event

      def sendNotification(user: User, event: EventMessage, topic: String): F[Unit] =
        notificationMessageProducer.produce(
          ProducerRecords.one(
            ProducerRecord(
              topic,
              user.slack_user_id,
              new NotificationMessage(user.slack_user_id, user.slack_channel_id, event.message)
            )
          )
        ) >> Async[F].unit
    }
}
