package kafka

import cats.effect.Concurrent
import cats.implicits._
import datatypes._
import fs2.Stream
import fs2.kafka._
import services.Slack

trait Streaming[F[_]] {
  def consumeMessages(topic: String): Stream[F, NotificationMessage]
}

object Streaming {

  def apply[F[_]](implicit ev: Streaming[F]): Streaming[F] = ev

  def impl[F[_]: Concurrent](
      maxConcurrent: Int,
      consumer: Stream[F, KafkaConsumer[F, String, NotificationMessage]],
      slack: Slack[F]
  ): Streaming[F] =
    new Streaming[F] {
      def consumeMessages(topic: String): Stream[F, NotificationMessage] =
        consumer
          .evalTap(_.subscribeTo(topic))
          .flatMap(_.stream)
          .mapAsync(maxConcurrent)(committable => processRecord(committable.record))

      private def processRecord(
          record: ConsumerRecord[String, NotificationMessage]
      ): F[NotificationMessage] =
        slack
          .sendMessage(SlackMessage(record.value.slackUserId, record.value.message))
          .as(record.value)
    }
}
