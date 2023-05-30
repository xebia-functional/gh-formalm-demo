package services

import cats.effect.Async
import cats.implicits._
import datatypes.{SlackMessage, SlackResponse}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s._
import org.typelevel.ci._
import org.typelevel.log4cats.Logger

trait Slack[F[_]] {
  def sendMessage(slackMessage: SlackMessage): F[Unit]
}

object Slack {
  def apply[F[_]](implicit ev: Slack[F]): Slack[F] = ev

  def impl[F[_]: Async](logger: Logger[F], httpClient: Client[F], slackTokenAPI: String): Slack[F] =
    new Slack[F] {
      private val headers: Headers = Headers(
        List(Header.Raw(ci"Authorization", "Bearer " + slackTokenAPI))
      )

      def sendMessage(slackMessage: SlackMessage): F[Unit] =
        httpClient
          .run(
            Request[F](
              method = Method.POST,
              headers = headers,
              uri = uri"https://slack.com/api/chat.postMessage"
            ).withEntity(slackMessage)
          )
          .use(res => {
            for {
              slackResponse <- res.as[SlackResponse]
              response <- (slackResponse.ok, slackResponse.error) match {
                case (true, _)        => Async[F].unit
                case (false, Some(e)) => logger.error(s"Error while notifying the Slack user: $e")
                case (false, None)    => logger.error("It seems Slack is failing without an error")
              }
            } yield response
          })
    }
}
