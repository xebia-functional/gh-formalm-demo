package api

import cats.effect.{Async, ExitCode, IO}
import config.ConfigService
import database.DatabaseService
import fs2.Stream
import fs2.kafka.{KafkaConsumer, KafkaProducer}
import github.GitHubService
import kafka.Streaming
import org.http4s.Uri
import org.http4s.implicits._
import org.http4s.server.Router
import services.Subscriptions
import org.http4s.server.middleware.Logger
import org.http4s.server.staticcontent.{FileService, fileService}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Server {

  def serve[F[_]: Async]: Stream[F, ExitCode] =
    for {
      config <- ConfigService.impl[F]
      producerSettings             = config.producerSettings
      notificationProducerSettings = config.notificationProducerSettings
      httpClient                   = config.httpClient
      dbTransactor                 = config.dbTransactor
      appLogger                    = Slf4jLogger.getLogger[F]
      httpMiddleware               = new Middleware[F](appLogger)
      baseUrl =
        Uri
          .fromString(config.github.baseUrl)
          .getOrElse(Uri.unsafeFromString("https://api.github.com/repos/"))

      _ = IO.println("Starting server...")

      kafkaProducer             <- KafkaProducer.stream(producerSettings)
      notificationKafkaProducer <- KafkaProducer.stream(notificationProducerSettings)
      kafkaConsumer = KafkaConsumer.stream(config.consumerSettings)

      databaseAlg = DatabaseService.impl[F](dbTransactor)

      subscriptionAlg = Subscriptions.impl[F](databaseAlg)
      streamingAlg = Streaming.impl[F](
        config.streaming.maxConcurrent,
        kafkaProducer,
        notificationKafkaProducer,
        kafkaConsumer,
        databaseAlg
      )
      githubAlg =
        GitHubService
          .impl[F](
            httpClient,
            baseUrl
          )

      _ = config.flyway.clean()
      _ = config.flyway.migrate()
      _ <- Stream.eval(config.configureCluster)

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = false)(
        Router(
          "api" -> new Routes[F](subscriptionAlg, githubAlg, streamingAlg)(httpMiddleware).routes,
          "docs" -> fileService(
            FileService.Config
              .apply[F](
                systemPath = "./subscriptions/src/main/resources/swagger-ui"
              )
          )
        ).orNotFound
      )

      _ <- Stream(
        streamingAlg.consumeMessages("events", "notifications"),
        config.httpServer.withHttpApp(finalHttpApp).serve
      ).parJoin(2)

    } yield ExitCode.Success
}
