package stubs

import cats.effect.Async
import datatypes.SlackMessage
import services.Slack

object SlackStub {
  def impl[F[_]: Async]: Slack[F] =
    new Slack[F] {
      override def sendMessage(slackMessage: SlackMessage): F[Unit] = {
        println(s"Sending message: ${slackMessage.text}")
        Async[F].unit
      }
    }
}
