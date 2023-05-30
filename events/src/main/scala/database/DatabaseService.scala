package database

import cats.effect.Async
import doobie.Transactor
import doobie.implicits._

trait DatabaseService[F[_]] {
  def checkActiveSubscriptions(organization: String, repository: String): F[Option[Int]]
  def getWebhookId(organization: String, repository: String): F[Option[Int]]
  def addRepository(organization: String, repository: String, webhookId: Int): F[Int]
  def removeRepository(organization: String, repository: String): F[Int]
  def incrementSubscription(organization: String, repository: String): F[Int]
  def decrementSubscription(organization: String, repository: String): F[Int]
}

object DatabaseService {

  def impl[F[_]: Async](xa: Transactor[F]): DatabaseService[F] =
    new DatabaseService[F] {
      def checkActiveSubscriptions(organization: String, repository: String): F[Option[Int]] =
        Queries
          .getSubscriptions(organization, repository)
          .option
          .transact(xa)

      def getWebhookId(organization: String, repository: String): F[Option[Int]] =
        Queries
          .getWebhookId(organization, repository)
          .option
          .transact(xa)

      def addRepository(organization: String, repository: String, webhookId: Int): F[Int] =
        Queries
          .insertRepository(organization, repository, webhookId)
          .run
          .transact(xa)

      def removeRepository(organization: String, repository: String): F[Int] =
        Queries
          .deleteRepository(organization, repository)
          .run
          .transact(xa)

      def incrementSubscription(organization: String, repository: String): F[Int] =
        Queries
          .incrementSubscription(organization, repository)
          .run
          .transact(xa)

      def decrementSubscription(organization: String, repository: String): F[Int] =
        Queries
          .decrementSubscription(organization, repository)
          .run
          .transact(xa)
    }
}
