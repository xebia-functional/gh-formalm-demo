package config

import cats.effect.Async
import datatypes._
import fs2.Stream
import fs2.kafka.vulcan._
import fs2.kafka._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import scala.concurrent.ExecutionContext.global

trait ConfigService[F[_]] {
  def httpClient: Client[F]
  def streaming: StreamingConfig
  def consumerSettings: ConsumerSettings[F, String, NotificationMessage]
  def slackTokenAPI: String
}

object ConfigService {
  def impl[F[_]: Async]: Stream[F, ConfigService[F]] =
    for {
      config <- Stream.eval(SetupConfig.loadConfig[F])
      client <- BlazeClientBuilder[F](global).stream
    } yield new ConfigService[F] {

      def httpClient: Client[F] = client

      def streaming: StreamingConfig = config.streaming

      private val avroSettings: AvroSettings[F] = AvroSettings {
        SchemaRegistryClientSettings[F](streaming.schemaRegistryUri)
      }

      implicit val notificationMessageDeserializer: RecordDeserializer[F, NotificationMessage] =
        avroDeserializer[NotificationMessage].using(avroSettings)

      def consumerSettings: ConsumerSettings[F, String, NotificationMessage] =
        ConsumerSettings[F, String, NotificationMessage]
          .withAutoOffsetReset(AutoOffsetReset.Latest)
          .withBootstrapServers(streaming.bootstrapServers)
          .withGroupId(streaming.consumerId)

      val slackTokenAPI: String = config.slackTokenAPI
    }
}
