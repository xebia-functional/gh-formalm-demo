package datatypes

import cats.implicits.catsSyntaxTuple3Semigroupal
import vulcan.Codec
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class NotificationMessage(
    slackUserId: String,
    slackChannelId: String,
    message: String
)

object NotificationMessage {
  implicit val notificationMessageCodec: Codec[NotificationMessage] =
    Codec.record(
      name = "NotificationMessage",
      namespace = "com.github.alerts"
    ) { field =>
      (
        field("slackId", _.slackUserId),
        field("slackChannelId", _.slackChannelId),
        field("message", _.message)
      ).mapN(NotificationMessage(_, _, _))
    }
}

case class SlackMessage(channel: String, text: String)

object SlackMessage {
  implicit val slackMessageEncoder: Encoder[SlackMessage] = deriveEncoder[SlackMessage]
}

case class SlackResponse(ok: Boolean, error: Option[String])

object SlackResponse {
  implicit val slackResponseDecoder: Decoder[SlackResponse] = deriveDecoder[SlackResponse]
}
