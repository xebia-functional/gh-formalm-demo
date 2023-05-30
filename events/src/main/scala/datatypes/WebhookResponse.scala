package datatypes

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class WebhookResponse(id: Int)

object WebhookResponse {
  implicit def webhookEventDecoder: Decoder[WebhookResponse] = deriveDecoder[WebhookResponse]
}
