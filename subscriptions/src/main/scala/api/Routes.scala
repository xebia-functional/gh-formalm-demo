package api

import cats.effect.Async
import cats.implicits._
import datatypes.KnownErrors.SlackError
import datatypes.SlackCommand.decodeSlackCommand
import datatypes.{Delete_Subscription, KafkaMessage, NewSubscription, RepositoryList, User}
import github.GitHubService
import kafka.Streaming
import org.http4s.{HttpRoutes, UrlForm}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import services.Subscriptions

final class Routes[F[_]: Async](s: Subscriptions[F], g: GitHubService[F], k: Streaming[F])(
    m: Middleware[F]
) extends Http4sDsl[F] {
  private def _routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "subscription" / user =>
        for {
          userId        <- User.parseOrRaise(user)
          subscriptions <- s.getSubscriptions(userId)
          response      <- Ok(subscriptions)
        } yield response

      case req @ POST -> Root / "subscription" / user =>
        for {
          userId         <- User.parseOrRaise(user)
          repositoryList <- req.as[RepositoryList]
          repositories   <- g.checkIfReposExist(repositoryList)
          subscriptions  <- s.postSubscriptions(userId, repositories)
          _ <- subscriptions.traverse(subscription => {
            k.sendMessage(
              "subscriptions",
              new KafkaMessage(
                userId.toString,
                NewSubscription,
                subscription.organization,
                subscription.repository
              )
            )
          })
          response <- Ok(subscriptions)
        } yield response

      case req @ DELETE -> Root / "subscription" / user =>
        for {
          userId        <- User.parseOrRaise(user)
          subscriptions <- req.as[RepositoryList].map(_.subscriptions)
          _             <- s.deleteSubscriptions(userId, subscriptions)
          _ <- subscriptions.traverse(subscription => {
            k.sendMessage(
              "subscriptions",
              new KafkaMessage(
                userId.toString,
                Delete_Subscription,
                subscription.organization,
                subscription.repository
              )
            )
          })
          response <- NoContent()
        } yield response

      case req @ POST -> Root / "subscription" / "slack" / "command" =>
        val response = for {
          slackCommand <-
            req
              .as[UrlForm]
              .map(_.values)
              .map(decodeSlackCommand)
          _             <- g.checkIfReposExist(slackCommand.repositories)
          slackResponse <- s.postSubscriptionsSlack(slackCommand)
          _ <- slackCommand.repositories.subscriptions.traverse(subscription => {
            k.sendMessage(
              "subscriptions",
              new KafkaMessage(
                slackCommand.user.user_id.toString,
                if (slackCommand.command.equalsIgnoreCase("subscribe")) NewSubscription
                else Delete_Subscription,
                subscription.organization,
                subscription.repository
              )
            )
          })
          response <- Accepted(slackResponse)
        } yield response
        response.handleErrorWith(e => Async[F].raiseError(SlackError(e.getMessage)))
    }

  def routes: HttpRoutes[F] = m(_routes)
}
