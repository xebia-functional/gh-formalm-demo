package datatypes

import java.util.UUID
object KnownErrors {

  sealed abstract class KnownErrors(val msg: String) extends Exception(msg)

  final case class UserNotFound(id: UUID) extends KnownErrors(s"user with id $id not found")

  final case object SubscriptionNotFound
      extends KnownErrors(
        s"You are not subscribed to one or more of the specified repositories"
      )

  final case class UUIDNotValid(id: String) extends KnownErrors(s"$id is not a valid UUID")

  final case class RepoNotFound(repository: Repository)
      extends KnownErrors(
        s"repository ${repository.organization}/${repository.repository} not found"
      )

  final case class CommandNotImplemented(message: String) extends KnownErrors(message)

  final case class SlackError(message: String) extends KnownErrors(message)

}
