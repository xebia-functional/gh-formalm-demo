package datatypes

import vulcan.{AvroError, Codec}

sealed trait OperationType
case object NewSubscription     extends OperationType
case object Delete_Subscription extends OperationType

object OperationType {
  implicit val operationTypeCodec: Codec[OperationType] =
    Codec.enumeration[OperationType](
      name = "OperationType",
      namespace = "com.github.alerts",
      symbols = List("subscribe", "unsubscribe"),
      encode = {
        case NewSubscription     => "subscribe"
        case Delete_Subscription => "unsubscribe"
      },
      decode = {
        case "subscribe"   => Right(NewSubscription)
        case "unsubscribe" => Right(Delete_Subscription)
        case other         => Left(AvroError(s"$other is not an operation"))
      },
      default = Some(NewSubscription)
    )
}
