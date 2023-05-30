package datatypes

import vulcan.{AvroError, Codec}

sealed trait OperationType
case object NEW_SUBSCRIPTION    extends OperationType
case object DELETE_SUBSCRIPTION extends OperationType

object OperationType {
  implicit val operationTypeCodec: Codec[OperationType] =
    Codec.enumeration[OperationType](
      name = "OperationType",
      namespace = "com.github.alerts",
      symbols = List("subscribe", "unsubscribe"),
      encode = {
        case NEW_SUBSCRIPTION    => "subscribe"
        case DELETE_SUBSCRIPTION => "unsubscribe"
      },
      decode = {
        case "subscribe"   => Right(NEW_SUBSCRIPTION)
        case "unsubscribe" => Right(DELETE_SUBSCRIPTION)
        case other         => Left(AvroError(s"$other is not an operation"))
      },
      default = Some(NEW_SUBSCRIPTION)
    )
}
