package services

import cats.effect.Sync
import database.DatabaseService
import datatypes.{Repository, SlackCommand, SlackResponse, Subscription, SubscriptionList}
import cats.implicits._
import datatypes.KnownErrors.{CommandNotImplemented, SubscriptionNotFound}

import java.util.UUID

trait Subscriptions[F[_]] {
  def getSubscriptions(id: UUID): F[SubscriptionList[Subscription]]
  def postSubscriptions(id: UUID, repositories: List[Repository]): F[List[Repository]]
  def deleteSubscriptions(id: UUID, repositories: List[Repository]): F[List[Repository]]
  def postSubscriptionsSlack(slackCommand: SlackCommand): F[SlackResponse]
}

object Subscriptions {
  def apply[F[_]](implicit ev: Subscriptions[F]): Subscriptions[F] = ev

  def impl[F[_]: Sync](db: DatabaseService[F]): Subscriptions[F] =
    new Subscriptions[F] {
      def getSubscriptions(userId: UUID): F[SubscriptionList[Subscription]] =
        for {
          _                <- db.checkUser(userId)
          subscriptionList <- db.getSubscriptions(userId)
        } yield subscriptionList

      def postSubscriptions(userId: UUID, repositories: List[Repository]): F[List[Repository]] =
        for {
          _     <- db.checkUser(userId)
          repos <- repositories.traverse(db.addRepository)
          subs <-
            repos
              .traverse(db.addSubscription(userId, _))
        } yield subs

      def deleteSubscriptions(userId: UUID, repositories: List[Repository]): F[List[Repository]] =
        for {
          _     <- db.checkUser(userId)
          check <- repositories.traverse(db.checkSubscription(userId, _))
          subs <-
            if (check.contains(false))
              Sync[F].raiseError(SubscriptionNotFound)
            else
              repositories.traverse(db.removeSubscription(userId, _))
        } yield subs

      def postSubscriptionsSlack(slackCommand: SlackCommand): F[SlackResponse] =
        if (slackCommand.command.equalsIgnoreCase("subscribe"))
          for {
            _               <- db.addUser(slackCommand.user)
            repos           <- slackCommand.repositories.subscriptions.traverse(db.addRepository)
            subscribedRepos <- repos.traverse(db.addSubscription(slackCommand.user.user_id, _))
          } yield SlackResponse(
            "ephemeral",
            s"You have been subscribed to ${subscribedRepos.mkString(", ")}!",
            List()
          )
        else if (slackCommand.command.equalsIgnoreCase("unsubscribe"))
          for {
            _ <- db.checkUser(slackCommand.user.user_id)
            subs <- slackCommand.repositories.subscriptions.traverse(
              db.checkSubscription(slackCommand.user.user_id, _)
            )
            unsubscribedRepos <-
              if (subs.contains(false))
                Sync[F].raiseError(SubscriptionNotFound)
              else
                slackCommand.repositories.subscriptions.traverse(
                  db.removeSubscription(slackCommand.user.user_id, _)
                )
          } yield SlackResponse(
            "ephemeral",
            s"You have been unsubscribed from ${unsubscribedRepos.mkString(", ")}!",
            List()
          )
        else
          Sync[F].raiseError(CommandNotImplemented("Command not supported yet!"))
    }
}
