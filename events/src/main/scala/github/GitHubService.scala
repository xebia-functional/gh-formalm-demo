package github

import cats.effect.Async
import datatypes.KnownErrors.WebhookError
import datatypes.{Config, Webhook, WebhookResponse}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.http4s.client.Client
import org.typelevel.ci._

trait GitHubService[F[_]] {
  def createWebhook(organization: String, repository: String): F[WebhookResponse]
  def removeWebhook(organization: String, repository: String, webhookId: Int): F[Boolean]
}

object GitHubService {

  def apply[F[_]](implicit ev: GitHubService[F]): GitHubService[F] = ev

  def impl[F[_]: Async](c: Client[F], githubTokenAPI: String, host: String): GitHubService[F] =
    new GitHubService[F] {
      val headers: Headers = Headers(
        List(
          Header.Raw(ci"Authorization", "Bearer " + githubTokenAPI),
          Header.Raw(ci"Content-Type", "application/json")
        )
      )

      val body: Webhook = Webhook(
        "web",
        List(
          "create",
          "delete",
          "issues",
          "issue_comment",
          "member",
          "pull_request",
          "push"
        ),
        Config(s"$host/webhook", "json")
      )

      def createWebhook(organization: String, repository: String): F[WebhookResponse] =
        c.run(
            Request[F](
              method = Method.POST,
              headers = headers,
              uri = Uri.unsafeFromString(
                s"https://api.github.com/repos/$organization/$repository/hooks"
              )
            ).withEntity(body)
          )
          .use(res =>
            res.status.isSuccess match {
              case true => res.as[WebhookResponse]
              case false if res.status.code == 422 =>
                Async[F]
                  .raiseError(WebhookError("Error when creating the webhook. Body malformed."))
              case false if List(401, 403).contains(res.status.code) =>
                Async[F]
                  .raiseError(WebhookError("Error when creating the webhook. Unauthorized."))
              case false => Async[F].raiseError(WebhookError("Error when creating the webhook."))
            }
          )

      def removeWebhook(organization: String, repository: String, webhookId: Int): F[Boolean] =
        c.run(
            Request[F](
              method = Method.DELETE,
              headers = headers,
              uri = Uri.unsafeFromString(
                s"https://api.github.com/repos/$organization/$repository/hooks/$webhookId"
              )
            )
          )
          .use(res =>
            res.status.isSuccess match {
              case true => Async[F].pure(true)
              case false if res.status.code == 404 =>
                Async[F].raiseError(WebhookError("Error when removing the webhook. Not found."))
              case false => Async[F].raiseError(WebhookError("Error when removing the webhook."))
            }
          )
    }
}
