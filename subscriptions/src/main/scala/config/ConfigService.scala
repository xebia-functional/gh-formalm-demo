package config

import cats.effect.{Async, Resource}
import cats.implicits._
import datatypes._
import doobie.hikari.HikariTransactor
import doobie.{ExecutionContexts, Transactor}
import fs2.Stream
import fs2.kafka._
import fs2.kafka.vulcan._
import org.apache.kafka.clients.admin.NewTopic
import org.flywaydb.core.Flyway
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.jdk.CollectionConverters.MapHasAsJava

trait ConfigService[F[_]] {
  def httpServer: BlazeServerBuilder[F]
  def httpClient: Client[F]
  def dbTransactor: Transactor[F]
  def streaming: StreamingConfig
  def github: GitHubConfig
  def flyway: Flyway
  def producerSettings: ProducerSettings[F, String, KafkaMessage]
  def notificationProducerSettings: ProducerSettings[F, String, NotificationMessage]
  def consumerSettings: ConsumerSettings[F, UUID, EventMessage]
  def configureCluster: F[Unit]
}

object ConfigService {
  private def createTransactor[F[_]: Async](
      url: String,
      user: String,
      password: String
  ): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        url,
        user,
        password,
        ce
      )
    } yield xa

  def impl[F[_]: Async]: Stream[F, ConfigService[F]] =
    for {
      config <- Stream.eval(SetupConfig.loadConfig[F])
      client <- BlazeClientBuilder[F](global).stream
      transactor <- Stream.resource(
        createTransactor(
          config.database.url,
          config.database.user,
          config.database.password
        )
      )
    } yield new ConfigService[F] {
      def httpServer: BlazeServerBuilder[F] =
        BlazeServerBuilder[F](ExecutionContext.global)
          .bindHttp(config.server.port, "0.0.0.0")

      def httpClient: Client[F] = client

      def dbTransactor: HikariTransactor[F] = transactor

      def streaming: StreamingConfig = config.streaming

      def github: GitHubConfig = config.github

      def flyway: Flyway =
        Flyway.configure
          .mixed(true)
          .baselineOnMigrate(true)
          .dataSource(
            config.database.url,
            config.database.user,
            config.database.password
          )
          .load

      private val avroSettings: AvroSettings[F] = AvroSettings {
        SchemaRegistryClientSettings[F](streaming.schemaRegistryUri)
      }

      implicit val kafkaMessageSerializer: RecordSerializer[F, KafkaMessage] =
        avroSerializer[KafkaMessage].using(avroSettings)

      implicit val notificationMessageSerializer: RecordSerializer[F, NotificationMessage] =
        avroSerializer[NotificationMessage].using(avroSettings)

      implicit val kafkaMessageDeserializer: RecordDeserializer[F, EventMessage] =
        avroDeserializer[EventMessage].using(avroSettings)

      def consumerSettings: ConsumerSettings[F, UUID, EventMessage] =
        ConsumerSettings[F, UUID, EventMessage]
          .withAutoOffsetReset(AutoOffsetReset.Latest)
          .withBootstrapServers(streaming.bootstrapServers)
          .withGroupId(streaming.consumerId)

      def producerSettings: ProducerSettings[F, String, KafkaMessage] =
        ProducerSettings[F, String, KafkaMessage]
          .withBootstrapServers(streaming.bootstrapServers)
          .withClientId(streaming.producerId)

      def notificationProducerSettings: ProducerSettings[F, String, NotificationMessage] =
        ProducerSettings[F, String, NotificationMessage]
          .withBootstrapServers(streaming.bootstrapServers)
          .withClientId(streaming.producerId)

      private def adminClientSettings: AdminClientSettings =
        AdminClientSettings.apply(streaming.bootstrapServers)

      private def kafkaAdminClientResource: Resource[F, KafkaAdminClient[F]] =
        KafkaAdminClient.resource(adminClientSettings)

      def configureCluster: F[Unit] =
        kafkaAdminClientResource.use { client =>
          for {
            existingTopics <- client.listTopics.names
            nonExistingTopics = (streaming.requiredOutputTopics ++ streaming.requiredInputTopics)
              .filter(!existingTopics.contains(_))
            _ <- nonExistingTopics.traverse(topicName =>
              client.createTopic(
                new NewTopic(topicName, 1, 1.toShort).configs(
                  Map("confluent.value.schema.validation" -> "true").asJava
                )
              )
            )
          } yield ()
        }
    }
}
