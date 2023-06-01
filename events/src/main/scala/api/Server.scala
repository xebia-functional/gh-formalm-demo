package api

import cats.effect.ExitCode
import config.ConfigService
import database.DatabaseService
import fs2.Stream
import fs2.kafka.{KafkaConsumer, KafkaProducer}
import github.GitHubService
import kafka.Streaming
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import cats.effect.Async
import org.typelevel.log4cats.slf4j.Slf4jLogger
// $COVERAGE-OFF$
object Server {

  def serve[F[_]: Async]: Stream[F, ExitCode] =
    for {
      config <- ConfigService.impl[F]
      client     = config.httpClient
      transactor = config.dbTransactor
      kafkaProducer <- KafkaProducer.stream(config.producerSettings)
      kafkaConsumer = KafkaConsumer.stream(config.consumerSettings)

      streamingAlg = Streaming.impl[F](
        config.streaming.maxConcurrent,
        kafkaProducer,
        kafkaConsumer,
        DatabaseService.impl[F](transactor),
        GitHubService.impl[F](client, config.githubTokenAPI, config.host)
      )

      logger     = Slf4jLogger.getLogger[F]
      middleware = new Middleware[F](logger)

      _ = config.flyway.clean()
      _ = config.flyway.migrate()

      _ <- Stream.eval(config.createTopicIfNotExists)

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = false)(
        new Routes[F](streamingAlg, "events")(middleware).routes.orNotFound
      )

      _ <- Stream(
        streamingAlg.consumeMessages("subscriptions"),
        config.httpServer.withHttpApp(finalHttpApp).serve
      ).parJoin(2)

    } yield ExitCode.Success
}
// $COVERAGE-ON$
