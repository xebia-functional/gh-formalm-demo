package datatypes

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class Webhook(name: String, events: List[String], config: Config)

final case class Config(url: String, content_type: String)

object Webhook {
  implicit val webhookDecoder: Decoder[Webhook] = deriveDecoder[Webhook]
  implicit val webhookEncoder: Encoder[Webhook] = deriveEncoder[Webhook]
}

object Config {
  implicit val configDecoder: Decoder[Config] = deriveDecoder[Config]
  implicit val configEncoder: Encoder[Config] = deriveEncoder[Config]
}
