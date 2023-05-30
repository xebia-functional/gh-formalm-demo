package stubs

import cats.effect.Async
import datatypes.WebhookResponse
import github.GitHubService

object GitHubServiceStub {
  def impl[F[_]: Async]: GitHubService[F] =
    new GitHubService[F] {
      def createWebhook(organization: String, repository: String): F[WebhookResponse] =
        Async[F].pure(WebhookResponse(47))
      def removeWebhook(organization: String, repository: String, webhookId: Int): F[Boolean] =
        Async[F].pure(true)
    }
}
