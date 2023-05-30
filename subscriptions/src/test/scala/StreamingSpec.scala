import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import database.DatabaseService
import datatypes.{EventMessage, KafkaMessage, NewSubscription, NotificationMessage}
import fs2.Stream
import fs2.kafka._
import fs2.kafka.vulcan._
import io.github.embeddedkafka.schemaregistry._
import kafka.Streaming
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.{DatabaseServiceStub, TestData}

import java.util.UUID

class StreamingSpec extends AnyFlatSpec with should.Matchers with EmbeddedKafka {

  val kafkaPort          = 12345
  val zooKeeperPort      = 12346
  val schemaRegistryPort = 12347

  val avroSettings: AvroSettings[IO] = AvroSettings(
    SchemaRegistryClientSettings[IO](s"http://localhost:$schemaRegistryPort")
  )

  implicit val eventMessageSerializer: RecordSerializer[IO, EventMessage] =
    avroSerializer[EventMessage].using(avroSettings)

  val producerSettingsEventMessage: ProducerSettings[IO, UUID, EventMessage] =
    ProducerSettings[IO, UUID, EventMessage]
      .withBootstrapServers(s"localhost:$kafkaPort")
      .withClientId("github-alerts-subscriptions-producer-test")

  val producerSettingsKafkaMessage: ProducerSettings[IO, String, KafkaMessage] =
    ProducerSettings[IO, String, KafkaMessage](
      avroSerializer[String].using(avroSettings),
      avroSerializer[KafkaMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withClientId("github-alerts-subscriptions-producer-test")

  val producerSettingsNotificationMessage: ProducerSettings[IO, String, NotificationMessage] =
    ProducerSettings[IO, String, NotificationMessage](
      avroSerializer[String].using(avroSettings),
      avroSerializer[NotificationMessage].using(avroSettings)
    ).withBootstrapServers(s"localhost:$kafkaPort")
      .withClientId("github-alerts-subscriptions-producer-test")

  implicit val kafkaMessageDeserializer: RecordDeserializer[IO, EventMessage] =
    avroDeserializer[EventMessage].using(avroSettings)

  val consumerSettingsEventMessage: ConsumerSettings[IO, UUID, EventMessage] =
    ConsumerSettings[IO, UUID, EventMessage]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(s"localhost:$kafkaPort")
      .withGroupId("github-alerts-subscriptions-consumer-test")

  val consumerSettingsKafkaMessage: ConsumerSettings[IO, String, KafkaMessage] =
    ConsumerSettings[IO, String, KafkaMessage](
      avroDeserializer[String].using(avroSettings),
      avroDeserializer[KafkaMessage].using(avroSettings)
    ).withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(s"localhost:$kafkaPort")
      .withGroupId("github-alerts-subscriptions-consumer-test")

  val consumerSettingsNotificationMessage: ConsumerSettings[IO, String, NotificationMessage] =
    ConsumerSettings[IO, String, NotificationMessage](
      avroDeserializer[String].using(avroSettings),
      avroDeserializer[NotificationMessage].using(avroSettings)
    ).withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(s"localhost:$kafkaPort")
      .withGroupId("github-alerts-subscriptions-consumer-test")

  val consumer: Stream[IO, KafkaConsumer[IO, UUID, EventMessage]] =
    KafkaConsumer.stream(consumerSettingsEventMessage)

  val producerKafkaMessage: Stream[IO, KafkaProducer[IO, String, KafkaMessage]] =
    KafkaProducer.stream(producerSettingsKafkaMessage)

  val producerNotificationMessage: Stream[IO, KafkaProducer[IO, String, NotificationMessage]] =
    KafkaProducer.stream(producerSettingsNotificationMessage)

  val eventMessage: EventMessage =
    EventMessage(
      "Test message for testing Kafka Embedded",
      "47deg",
      "github-alerts-scala-david.corral"
    )
  val kafkaMessage: KafkaMessage = KafkaMessage(
    TestData.dummyUser.slack_user_id,
    NewSubscription,
    "47deg",
    "github-alerts-scala-david.corral"
  )
  val notificationMessage: NotificationMessage = NotificationMessage(
    TestData.dummyUser.slack_user_id,
    TestData.dummyUser.slack_channel_id,
    "Test message for testing Kafka Embedded"
  )

  val topic     = "test-kafka-embedded"
  val topicSink = "test-kafka-embedded-sink"

  val databaseAlg: DatabaseService[IO] =
    DatabaseServiceStub.impl[IO](TestData.dummyUser, TestData.dummySubscriptions)

  val uuid: UUID = UUID.fromString("f000aa01-0451-4000-b000-000000000000")

  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(
      kafkaPort = kafkaPort,
      zooKeeperPort = zooKeeperPort,
      schemaRegistryPort = schemaRegistryPort
    )

  val embeddedKafka: Resource[IO, EmbeddedKWithSR] =
    Resource.make(IO(EmbeddedKafka.start()))(kafka => IO(kafka.stop(true)))

  "Send EventMessage to Kafka and consume it using the algebra" should "be successful" in {
    val res: Stream[IO, (EventMessage, NotificationMessage)] =
      Stream.resource(embeddedKafka).flatMap { _ =>
        for {
          producerNM <- producerNotificationMessage
          producerKM <- producerKafkaMessage
          streamingAlg = Streaming.impl[IO](25, producerKM, producerNM, consumer, databaseAlg)
          _ <-
            KafkaProducer
              .stream(producerSettingsEventMessage)
              .evalTap(_.produce(ProducerRecords.one(ProducerRecord(topic, uuid, eventMessage))))
          eventMessage <- streamingAlg.consumeMessages(topic, topicSink).take(1)
          notificationMessage <-
            KafkaConsumer
              .stream(consumerSettingsNotificationMessage)
              .evalTap(_.subscribeTo(topicSink))
              .evalTap(_.seekToBeginning)
              .flatMap(_.stream)
              .map(_.record.value)
              .take(1)
        } yield (eventMessage, notificationMessage)
      }

    res.compile.lastOrError.unsafeRunSync()._1 should be(eventMessage)
    res.compile.lastOrError.unsafeRunSync()._2 should be(notificationMessage)
  }

  "Send KafkaMessage to Kafka using the algebra and consume it" should "be successful" in {
    val res: Stream[IO, KafkaMessage] = Stream.resource(embeddedKafka).flatMap { _ =>
      for {
        producerNM <- producerNotificationMessage
        producerKM <- producerKafkaMessage
        streamingAlg = Streaming.impl[IO](25, producerKM, producerNM, consumer, databaseAlg)
        _            = streamingAlg.sendMessage(topic, kafkaMessage).unsafeRunSync()
        message <-
          KafkaConsumer
            .stream(consumerSettingsKafkaMessage)
            .evalTap(_.subscribeTo(topic))
            .evalTap(_.seekToBeginning)
            .flatMap(_.stream)
            .map(_.record.value)
            .take(1)
      } yield message
    }

    res.compile.lastOrError.unsafeRunSync() should be(kafkaMessage)
  }

}
