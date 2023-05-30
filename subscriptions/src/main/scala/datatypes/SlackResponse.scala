package datatypes

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class SlackResponse(
    response_type: String,
    text: String,
    blocks: List[Block]
)

final case class Block(`type`: String, text: Text)

final case class Text(`type`: String, text: String)

object SlackResponse {
  implicit val slackResponseEncoder: Encoder[SlackResponse] =
    deriveEncoder[SlackResponse]
}

object Block {
  implicit val blockEncoder: Encoder[Block] =
    deriveEncoder[Block]
}

object Text {
  implicit val textEncoder: Encoder[Text] =
    deriveEncoder[Text]
}
