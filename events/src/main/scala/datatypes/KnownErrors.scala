package datatypes

object KnownErrors {
  sealed abstract class KnownErrors(val msg: String)      extends Exception(msg)
  final case class WebhookError(message: String)          extends KnownErrors(message)
  final case class CommandNotImplemented(message: String) extends KnownErrors(message)
  final case class InvalidRepoName(message: String)       extends KnownErrors(message)
}
