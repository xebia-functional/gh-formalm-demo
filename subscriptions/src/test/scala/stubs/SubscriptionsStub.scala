package stubs

import cats.ApplicativeError
import datatypes.KnownErrors.UserNotFound
import datatypes._
import services.Subscriptions

import java.util.UUID

object SubscriptionsStub {
  def impl[F[_]](
      dummyUser: User,
      dummySubscriptions: SubscriptionList[Subscription]
  )(implicit ae: ApplicativeError[F, Throwable]): Subscriptions[F] =
    new Subscriptions[F] {
      def getSubscriptions(userId: UUID): F[SubscriptionList[Subscription]] =
        if (userId.equals(dummyUser.user_id))
          ae.pure(dummySubscriptions)
        else
          ae.raiseError(UserNotFound(userId))

      def postSubscriptions(
          id: UUID,
          repositories: List[Repository]
      ): F[List[Repository]] = ae.pure(repositories)

      def deleteSubscriptions(
          id: UUID,
          repositories: List[Repository]
      ): F[List[Repository]] =
        if (id.equals(dummyUser.user_id))
          ae.pure(repositories)
        else
          ae.raiseError(UserNotFound(id))

      def postSubscriptionsSlack(slackCommand: SlackCommand): F[SlackResponse] =
        ???
    }
}
