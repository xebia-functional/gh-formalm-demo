package stubs

import cats.ApplicativeError
import database.DatabaseService
import datatypes.KnownErrors.{SubscriptionNotFound, UserNotFound}
import datatypes._

import java.util.UUID

object DatabaseServiceStub {
  def impl[F[_]](
      user: User,
      subscriptionList: SubscriptionList[Subscription]
  )(implicit ae: ApplicativeError[F, Throwable]): DatabaseService[F] =
    new DatabaseService[F] {
      def checkUser(userId: UUID): F[UUID] =
        if (userId.toString.equalsIgnoreCase(user.user_id.toString))
          ae.pure(userId)
        else
          ae.raiseError(UserNotFound(userId))

      def getUser(userId: UUID): F[User] =
        if (userId.toString.equalsIgnoreCase(user.user_id.toString))
          ae.pure(user)
        else
          ae.raiseError(UserNotFound(userId))

      def addUser(user: User): F[Boolean] = ae.pure(true)

      def addRepository(repository: Repository): F[Repository] =
        ae.pure(repository)

      def getSubscriptions(userId: UUID): F[SubscriptionList[Subscription]] =
        if (userId.toString.equalsIgnoreCase(user.user_id.toString))
          ae.pure(subscriptionList)
        else
          ae.raiseError(UserNotFound(userId))

      def addSubscription(userId: UUID, repository: Repository): F[Repository] =
        if (userId.toString.equalsIgnoreCase(user.user_id.toString))
          ae.pure(repository)
        else
          ae.raiseError(UserNotFound(userId))

      def removeSubscription(userId: UUID, repository: Repository): F[Repository] =
        if (userId.toString.equalsIgnoreCase(user.user_id.toString))
          if (
            subscriptionList.subscriptions
              .exists(s => s.repository == repository)
          )
            ae.pure(repository)
          else
            ae.pure(repository)
        else
          ae.raiseError(UserNotFound(userId))

      def checkSubscription(userId: UUID, repository: Repository): F[Boolean] =
        if (
          userId.toString.equalsIgnoreCase(user.user_id.toString) &&
          subscriptionList.subscriptions.exists(_.repository == repository)
        )
          ae.pure(true)
        else
          ae.raiseError(SubscriptionNotFound)

      def getSubscribedUsers(repoId: UUID): F[List[User]] = ae.pure(List(user))
    }
}
