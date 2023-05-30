import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import datatypes.NotificationMessage
import fs2.Stream
import fs2.kafka.vulcan._
import fs2.kafka._
import io.github.embeddedkafka.schemaregistry._
import kafka.Streaming
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import services.Slack
import stubs.SlackStub

class StreamingSpec extends AnyFlatSpec with should.Matchers with EmbeddedKafka {

  "Send message to Kafka and consume it using the algebra" should "be successful" in {
    implicit val config: EmbeddedKafkaConfig =
      EmbeddedKafkaConfig(kafkaPort = 12345, zooKeeperPort = 12346, schemaRegistryPort = 12347)

    val embeddedKafka: Resource[IO, EmbeddedKWithSR] =
      Resource.make(IO(EmbeddedKafka.start()))(kafka => IO(kafka.stop(true)))

    val topic = "test-kafka-embedded"
    val notificationMessage = NotificationMessage(
      "SDO47DEG",
      "LND47DEG",
      "This is a message from the future... You made it! You were able to test Kafka Embedded with fs2 Kafka."
    )

    val res: Stream[IO, NotificationMessage] = Stream.resource(embeddedKafka).flatMap { kafka =>
      val actualConfig: EmbeddedKafkaConfig = kafka.config

      val avroSettings: AvroSettings[IO] =
        AvroSettings {
          SchemaRegistryClientSettings[IO](
            s"http://localhost:${actualConfig.schemaRegistryPort}"
          )
        }

      val consumerSettings: ConsumerSettings[IO, String, NotificationMessage] =
        ConsumerSettings[IO, String, NotificationMessage](
          avroDeserializer[String].using(avroSettings),
          avroDeserializer[NotificationMessage].using(avroSettings)
        ).withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withEnableAutoCommit(true)
          .withGroupId("github-alerts-notifications-consumer-test")

      val producerSettings: ProducerSettings[IO, String, NotificationMessage] =
        ProducerSettings[IO, String, NotificationMessage](
          avroSerializer[String].using(avroSettings),
          avroSerializer[NotificationMessage].using(avroSettings)
        ).withBootstrapServers(s"localhost:${actualConfig.kafkaPort}")
          .withClientId("github-alerts-notifications-producer-test")

      val slackAlg: Slack[IO] = SlackStub.impl[IO]
      val consumer: Stream[IO, KafkaConsumer[IO, String, NotificationMessage]] = KafkaConsumer
        .stream(consumerSettings.withBootstrapServers(s"localhost:${actualConfig.kafkaPort}"))
      val streamingAlg: Streaming[IO] = Streaming.impl[IO](25, consumer, slackAlg)

      for {
        _ <-
          KafkaProducer
            .stream(producerSettings.withBootstrapServers(s"localhost:${actualConfig.kafkaPort}"))
            .evalTap(_.produce(ProducerRecords.one(ProducerRecord(topic, "", notificationMessage))))
        message <- streamingAlg.consumeMessages(topic).take(1)
      } yield message
    }

    res.compile.lastOrError.unsafeRunSync() should be(notificationMessage)
  }
}
