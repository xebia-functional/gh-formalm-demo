package datatypes

import cats.implicits.catsSyntaxTuple4Semigroupal
import vulcan.Codec

final case class KafkaMessage(
    userId: String,
    operationType: OperationType,
    organization: String,
    repository: String
)

object KafkaMessage {
  implicit val kafkaMessageCodec: Codec[KafkaMessage] =
    Codec.record(
      name = "KafkaMessage",
      namespace = "com.github.alerts"
    ) { field =>
      (
        field("userId", _.userId),
        field("operationType", _.operationType),
        field("organization", _.organization),
        field("repository", _.repository)
      ).mapN(KafkaMessage(_, _, _, _))
    }
}
