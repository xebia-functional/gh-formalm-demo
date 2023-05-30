package datatypes

import cats.implicits.catsSyntaxTuple3Semigroupal
import vulcan.Codec

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
