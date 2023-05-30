package datatypes

import io.circe.generic.semiauto.{deriveDecoder}
import io.circe.syntax.KeyOps
import io.circe.{Decoder, Encoder, Json}

import java.time.LocalDateTime

case class Subscription(
    repository: Repository,
    subscribedAt: LocalDateTime
)

object Subscription {
  implicit val subscriptionDecoder: Decoder[Subscription] =
    deriveDecoder[Subscription]
  implicit val subscriptionEncoder: Encoder[Subscription] =
    Encoder.instance(s =>
      Json.obj(
        "organization" := s.repository.organization,
        "repository" := s.repository.repository,
        "subscribedAt" := s.subscribedAt
      )
    )
}
