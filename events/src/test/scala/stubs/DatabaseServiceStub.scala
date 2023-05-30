package stubs

import cats.effect.Async
import database.DatabaseService

object DatabaseServiceStub {
  def impl[F[_]: Async]: DatabaseService[F] =
    new DatabaseService[F] {
      def checkActiveSubscriptions(organization: String, repository: String): F[Option[Int]] =
        Async[F].pure(Some(1))
      def getWebhookId(organization: String, repository: String): F[Option[Int]] =
        Async[F].pure(Some(1))
      def addRepository(organization: String, repository: String, webhookId: Int): F[Int] =
        Async[F].pure(1)
      def removeRepository(organization: String, repository: String): F[Int]      = Async[F].pure(1)
      def incrementSubscription(organization: String, repository: String): F[Int] = Async[F].pure(1)
      def decrementSubscription(organization: String, repository: String): F[Int] = Async[F].pure(1)
    }
}
