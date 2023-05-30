import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import database.DatabaseService
import datatypes.{EventMessage, KafkaMessage, NEW_SUBSCRIPTION}
import fs2.Stream
import fs2.kafka._
import fs2.kafka.vulcan._
import github.GitHubService
import io.github.embeddedkafka.schemaregistry._
import kafka.Streaming
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.{DatabaseServiceStub, GitHubServiceStub}

import java.util.UUID

class StreamingSpec extends AnyFlatSpec with should.Matchers with EmbeddedKafka {

  val kafkaPort          = 12345
  val zooKeeperPort      = 12346
  val schemaRegistryPort = 12347

  val avroSettings: AvroSettings[IO] = AvroSettings(
    SchemaRegistryClientSettings[IO](s"http://localhost:$schemaRegistryPort")
  )

  val producerSettingsKafkaMessage: ProducerSettings[IO, String, KafkaMessage] =
    ProducerSettings[IO, String, KafkaMessage](
      avroSerializer[String].using(avroSettings),
      avroSerializer[KafkaMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withClientId("github-alerts-events-producer-test")

  val consumerSettingsKafkaMessage: ConsumerSettings[IO, String, KafkaMessage] =
    ConsumerSettings[IO, String, KafkaMessage](
      avroDeserializer[String].using(avroSettings),
      avroDeserializer[KafkaMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withGroupId("github-alerts-events-consumer-test")

  val producerSettingsEventMessage: ProducerSettings[IO, UUID, EventMessage] =
    ProducerSettings[IO, UUID, EventMessage](
      avroSerializer[UUID].using(avroSettings),
      avroSerializer[EventMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withClientId("github-alerts-events-producer-test")

  val consumerSettingsEventMessage: ConsumerSettings[IO, String, EventMessage] =
    ConsumerSettings[IO, String, EventMessage](
      avroDeserializer[String].using(avroSettings),
      avroDeserializer[EventMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withGroupId("github-alerts-events-consumer-test")

  val consumer: Stream[IO, KafkaConsumer[IO, String, KafkaMessage]] =
    KafkaConsumer.stream(consumerSettingsKafkaMessage)

  val producer: Stream[IO, KafkaProducer[IO, UUID, EventMessage]] =
    KafkaProducer.stream(producerSettingsEventMessage)

  val topic = "test-kafka-embedded"

  val databaseAlg: DatabaseService[IO] = DatabaseServiceStub.impl[IO]
  val githubAlg: GitHubService[IO]     = GitHubServiceStub.impl[IO]

  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(
      kafkaPort = kafkaPort,
      zooKeeperPort = zooKeeperPort,
      schemaRegistryPort = schemaRegistryPort
    )

  val embeddedKafka: Resource[IO, EmbeddedKWithSR] =
    Resource.make(IO(EmbeddedKafka.start()))(kafka => IO(kafka.stop(true)))

  "Send KafkaMessage to Kafka and consume it using the algebra" should "be successful" in {
    val kafkaMessage =
      KafkaMessage("SFO47DEG", NEW_SUBSCRIPTION, "47deg", "github-alerts-scala-david.corral")

    val res: Stream[IO, KafkaMessage] = Stream.resource(embeddedKafka).flatMap { _ =>
      for {
        prod <- producer
        streamingAlg = Streaming.impl[IO](25, prod, consumer, databaseAlg, githubAlg)
        _ <-
          KafkaProducer
            .stream(producerSettingsKafkaMessage)
            .evalTap(_.produce(ProducerRecords.one(ProducerRecord(topic, "", kafkaMessage))))
        message <- streamingAlg.consumeMessages(topic).take(1)
      } yield message
    }

    res.compile.lastOrError.unsafeRunSync() should be(kafkaMessage)
  }

  "Send EventMessage using the algebra and consume it" should "be successful" in {
    val eventMessage =
      EventMessage("This is a test event message", "47deg", "github-alerts-scala-david.corral")

    val res: Stream[IO, EventMessage] = Stream.resource(embeddedKafka).flatMap { _ =>
      for {
        prod <- producer
        streamingAlg = Streaming.impl[IO](25, prod, consumer, databaseAlg, githubAlg)
        _            = streamingAlg.sendMessage(topic, eventMessage).unsafeRunSync()
        message <-
          KafkaConsumer
            .stream(consumerSettingsEventMessage)
            .evalTap(_.subscribeTo(topic))
            .evalTap(_.seekToBeginning)
            .flatMap(_.stream)
            .map(_.record.value)
            .take(1)
      } yield message
    }

    res.compile.lastOrError.unsafeRunSync() should be(eventMessage)
  }
}
