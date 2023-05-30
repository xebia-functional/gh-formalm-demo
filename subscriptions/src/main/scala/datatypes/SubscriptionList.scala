package datatypes

import io.circe.syntax.KeyOps
import io.circe._

final case class SubscriptionList[A](subscriptions: List[A]) extends AnyVal
object SubscriptionList {
  implicit def subscriptionListDecoder[A: Decoder]: Decoder[SubscriptionList[A]] =
    Decoder.decodeList[A].map(SubscriptionList(_))
  implicit def subscriptionListEncoder[A: Encoder]: Encoder[SubscriptionList[A]] =
    Encoder.instance(s =>
      Json.obj(
        "subscriptions" := s.subscriptions
      )
    )
}
