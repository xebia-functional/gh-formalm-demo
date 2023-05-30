package database

import cats.effect.Async
import datatypes.{Repository, Subscription, SubscriptionList, User}
import doobie.Transactor
import doobie.implicits._

import java.time.LocalDateTime
import java.util.UUID

trait DatabaseService[F[_]] {
  def checkUser(userId: UUID): F[UUID]
  def getUser(userId: UUID): F[User]
  def addUser(user: User): F[Boolean]

  def addRepository(repository: Repository): F[Repository]

  def getSubscriptions(userId: UUID): F[SubscriptionList[Subscription]]
  def addSubscription(userId: UUID, repository: Repository): F[Repository]
  def removeSubscription(userId: UUID, repository: Repository): F[Repository]
  def checkSubscription(userId: UUID, repository: Repository): F[Boolean]

  def getSubscribedUsers(repoId: UUID): F[List[User]]
}

object DatabaseService {

  def impl[F[_]: Async](
      xa: Transactor[F]
  ): DatabaseService[F] =
    new DatabaseService[F] {
      def checkUser(userId: UUID): F[UUID] =
        Queries
          .userExists(userId)
          .unique
          .transact(xa)

      def getUser(userId: UUID): F[User] =
        Queries
          .getUser(userId)
          .unique
          .transact(xa)

      def addUser(user: User): F[Boolean] =
        Queries
          .insertUser(user.user_id, user.slack_user_id, user.slack_channel_id)
          .run
          .map(_ > 0)
          .transact(xa)

      def addRepository(repository: Repository): F[Repository] =
        Queries
          .insertRepository(
            repository.getUUID,
            repository.organization,
            repository.repository
          )
          .run
          .map(_ => repository)
          .transact(xa)

      def getSubscriptions(userId: UUID): F[SubscriptionList[Subscription]] =
        Queries
          .getSubscriptions(userId)
          .to[List]
          .map(SubscriptionList(_))
          .transact(xa)

      def addSubscription(userId: UUID, repository: Repository): F[Repository] =
        Queries
          .insertSubscription(
            userId,
            repository.getUUID,
            LocalDateTime.now(),
            0
          )
          .run
          .map(_ => repository)
          .transact(xa)

      def removeSubscription(userId: UUID, repository: Repository): F[Repository] =
        Queries
          .removeSubscription(userId, repository.getUUID)
          .run
          .map(_ => repository)
          .transact(xa)

      def checkSubscription(userId: UUID, repository: Repository): F[Boolean] =
        Queries
          .checkSubscription(userId, repository.getUUID)
          .unique
          .map(_ > 0)
          .transact(xa)

      def getSubscribedUsers(repoId: UUID): F[List[User]] =
        Queries
          .getSubscribedUsers(repoId)
          .nel
          .map(_.toList)
          .transact(xa)
    }
}
