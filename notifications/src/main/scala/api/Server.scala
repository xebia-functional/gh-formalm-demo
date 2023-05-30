package api

import cats.effect.{Async, ExitCode}
import config.ConfigService
import fs2.Stream
import fs2.kafka.KafkaConsumer
import kafka.Streaming
import services.Slack
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Server {

  def serve[F[_]: Async]: Stream[F, ExitCode] =
    for {
      config <- ConfigService.impl[F]
      httpClient    = config.httpClient
      slackTokenAPI = Slack.impl(Slf4jLogger.getLogger[F], httpClient, config.slackTokenAPI)
      kafkaConsumer = KafkaConsumer.stream(config.consumerSettings)
      streamingAlg  = Streaming.impl[F](config.streaming.maxConcurrent, kafkaConsumer, slackTokenAPI)
      _ <- streamingAlg.consumeMessages("notifications")
    } yield ExitCode.Success
}
