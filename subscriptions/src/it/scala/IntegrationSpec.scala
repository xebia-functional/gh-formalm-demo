import api.{Middleware, Routes}
import cats.effect.{IO, Resource}
import database.DatabaseService
import datatypes._
import doobie.hikari.HikariTransactor
import fs2.Stream
import fs2.kafka._
import fs2.kafka.vulcan._
import github.GitHubService
import io.circe.Json
import io.circe.syntax.EncoderOps
import kafka.Streaming
import org.flywaydb.core.Flyway
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.implicits._
import services.Subscriptions

import java.util.UUID
import scala.concurrent.ExecutionContext.global

@munit.IgnoreSuite
class IntegrationSpec extends CatsEffectSuite {

  val flyway: Flyway = Flyway.configure
    .mixed(true)
    .baselineOnMigrate(true)
    .dataSource(
      sys.env.getOrElse("DB_TEST", "jdbc:postgresql:github_alerts"),
      sys.env.getOrElse("USER_TEST", "postgres"),
      sys.env.getOrElse("PASSWORD_TEST", "postgres")
    )
    .load

  flyway.clean()
  flyway.migrate()

  val transactor: Resource[IO, HikariTransactor[IO]] = for {
    be <- Blocker[IO]
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      sys.env.getOrElse("DB_TEST", "jdbc:postgresql:github_alerts"),
      sys.env.getOrElse("USER_TEST", "postgres"),
      sys.env.getOrElse("PASSWORD_TEST", "postgres"),
      global,
      be
    )
  } yield xa

  test("POST /subscription/slack/command 200") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/slack/command",
        Left(
          UrlForm(
            "command"      -> "/subscribe",
            "text"         -> "47deg/scalacon21-slides",
            "response_url" -> "https://hooks.slack.com/commands/T024G6JTP/2182982874551/NEa5zJt14LuQaUwvrLeujAQH",
            "trigger_id"   -> "2190958771078.2152222941.c9f82c13c8f2323b328f5a2c8e821579",
            "user_id"      -> "T017DHDS1B8",
            "channel_id"   -> "L0SJSDF71FC"
          )
        )
      ).map(_.status),
      Status.Accepted
    )
  }

  test("GET /subscription/<existingUUID> 200") {
    assertIO(
      prepareRequest(
        Method.GET,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right("".asJson)
      ).map(_.status),
      Status.Ok
    )
  }

  test("GET /subscription/<nonExistingUUID> 404") {
    assertIO(
      prepareRequest(
        Method.GET,
        uri"/subscription/3573102a-fe97-4224-a1f3-bd38760fd061",
        Right("".asJson)
      ).map(_.status),
      Status.NotFound
    )
  }

  test("GET /subscription/<invalidUUID> 500") {
    assertIO(
      prepareRequest(
        Method.GET,
        uri"/subscription/i-n-v-a-l-i-d-UU-ID",
        Right("".asJson)
      ).map(_.status),
      Status.InternalServerError
    )
  }

  test("POST /subscription/<uuid> 200") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right(
          RepositoryList(List(Repository("47deg", "47deg.github.io"))).asJson
        )
      ).map(_.status),
      Status.Ok
    )
  }

  test("POST /subscription/<uuid> 400 (non existing repo)") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right(
          RepositoryList(
            List(Repository("47deg", "non-existing-repo"))
          ).asJson
        )
      ).map(_.status),
      Status.BadRequest
    )
  }

  test("POST /subscription/<uuid> 400 (invalid body)") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right("".asJson)
      ).map(_.status),
      Status.BadRequest
    )
  }

  test("POST /subscription/<invalidUUID> 500") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/i-n-v-a-l-i-d-UU-ID",
        Right(
          RepositoryList(
            List(Repository("47deg", "scalacon21-slides"))
          ).asJson
        )
      ).map(_.status),
      Status.InternalServerError
    )
  }

  test("DELETE /subscription/<uuid> 204") {
    assertIO(
      prepareRequest(
        Method.DELETE,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right(
          RepositoryList(
            List(
              Repository("47deg", "scalacon21-slides"),
              Repository("47deg", "47deg.github.io")
            )
          ).asJson
        )
      ).map(_.status),
      Status.NoContent
    )
  }

  test("DELETE /subscription/<uuid> 400") {
    assertIO(
      prepareRequest(
        Method.DELETE,
        uri"/subscription/5aafd6d1-7fa8-3a6f-9041-adc05f5f54ff",
        Right("".asJson)
      ).map(_.status),
      Status.BadRequest
    )
  }

  test("DELETE /subscription/<uuid> 404") {
    assertIO(
      prepareRequest(
        Method.DELETE,
        uri"/subscription/3573102a-fe97-4224-a1f3-bd38760fd061",
        Right(RepositoryList(List(Repository("47deg", "thool"))).asJson)
      ).map(_.status),
      Status.NotFound
    )
  }

  test("DELETE /subscription/<invalidUUID> 500") {
    assertIO(
      prepareRequest(
        Method.DELETE,
        uri"/subscription/invalid-U-U-I-D",
        Right(RepositoryList(List(Repository("47deg", "thool"))).asJson)
      ).map(_.status),
      Status.InternalServerError
    )
  }

  test("POST /subscription/slack/command 500") {
    assertIO(
      prepareRequest(
        Method.POST,
        uri"/subscription/slack/command",
        Left(
          UrlForm(
            "incorrectForm" -> "/subscribe"
          )
        )
      ).map(_.status),
      Status.Accepted
    )
  }

  def prepareRequest(
      method: Method,
      uri: Uri,
      body: Either[UrlForm, Json]
  ): IO[Response[IO]] = {
    performRequest(
      body match {
        case Left(urlForm) =>
          Request[IO](method, uri).withEntity(
            urlForm
          )
        case Right(json) =>
          Request[IO](method, uri).withEntity(
            json
          )
      }
    )
  }

  def performRequest(request: Request[IO]): IO[Response[IO]] =
    transactor.use(t =>
      BlazeClientBuilder[IO](global).resource.use { client =>
        KafkaProducer.resource(producerSettings).use {
          kafkaMessageProducer =>
            KafkaProducer.resource(notificationProducerSettings).use {
              notificationMessageProducer =>
                val databaseAlg: DatabaseService[IO] = DatabaseService.impl[IO](t)
                val consumer: Stream[IO, KafkaConsumer[IO, UUID, EventMessage]] =
                  KafkaConsumer.stream(consumerSettings)
                val streamingAlg: Streaming[IO] = Streaming.impl[IO](
                  25,
                  kafkaMessageProducer,
                  notificationMessageProducer,
                  consumer,
                  databaseAlg
                )
                val subscriptionAlg: Subscriptions[IO] = Subscriptions.impl[IO](databaseAlg)
                val githubAlg: GitHubService[IO] =
                  GitHubService.impl[IO](client, uri"https://api.github.com/repos/")
                val logger     = Slf4jLogger.getLogger[IO]
                val middleware = new Middleware[IO](logger)
                new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(
                  middleware
                ).routes.orNotFound
                  .run(request)
            }
        }
      }
    )

  private val avroSettings: AvroSettings[IO] = AvroSettings {
    SchemaRegistryClientSettings[IO]("http://localhost:8081")
  }

  implicit val kafkaMessageSerializer: RecordSerializer[IO, KafkaMessage] =
    avroSerializer[KafkaMessage].using(avroSettings)

  implicit val notificationMessageSerializer: RecordSerializer[IO, NotificationMessage] =
    avroSerializer[NotificationMessage].using(avroSettings)

  implicit val kafkaMessageDeserializer: RecordDeserializer[IO, EventMessage] =
    avroDeserializer[EventMessage].using(avroSettings)

  def producerSettings: ProducerSettings[IO, String, KafkaMessage] =
    ProducerSettings[IO, String, KafkaMessage]
      .withBootstrapServers("localhost:9092")
      .withClientId("github-alerts-producer")

  def notificationProducerSettings: ProducerSettings[IO, String, NotificationMessage] =
    ProducerSettings[IO, String, NotificationMessage]
      .withBootstrapServers("localhost:9092")
      .withClientId("github-alerts-producer")

  def consumerSettings: ConsumerSettings[IO, UUID, EventMessage] =
    ConsumerSettings[IO, UUID, EventMessage]
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withBootstrapServers("localhost:9092")
      .withGroupId("github-alerts-consumer")
}
