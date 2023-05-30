package database

import datatypes.{Subscription, User}
import doobie.{Query0, Update0}
import doobie.implicits._
import doobie.postgres.implicits._

import java.time.LocalDateTime
import java.util.UUID

object Queries {

  def getUser(user_id: UUID): Query0[User] =
    sql"""
        SELECT slack_user_id, slack_channel_id, user_id
        FROM users
        WHERE user_id=$user_id
      """.query[User]

  def userExists(user_id: UUID): Query0[UUID] =
    sql"""
        SELECT user_id
        FROM users
        WHERE user_id=$user_id
      """.query[UUID]

  def getSubscriptions(user_id: UUID): Query0[Subscription] =
    sql"""
        SELECT r.organization, r.repository, s.subscribed_at
        FROM subscriptions s, repositories r
        WHERE s.user_id=$user_id and r.repository_id = s.repository_id
      """.query[Subscription]

  def insertUser(
      user_id: UUID,
      slack_user_id: String,
      slack_channel_id: String
  ): Update0 =
    sql"""insert into users (user_id, slack_user_id, slack_channel_id)
         values ($user_id,$slack_user_id, $slack_channel_id) ON CONFLICT DO NOTHING""".update

  def insertRepository(
      repository_id: UUID,
      organization: String,
      repository: String
  ): doobie.Update0 =
    sql"""insert into repositories (repository_id, organization, repository)
         values ($repository_id, $organization, $repository) ON CONFLICT DO NOTHING""".update

  def insertSubscription(
      user_id: UUID,
      repository_id: UUID,
      subscribed_at: LocalDateTime,
      mode: Int
  ): doobie.Update0 =
    sql"""insert into subscriptions (user_id, repository_id, subscribed_at, mode)
         values ($user_id, $repository_id, $subscribed_at, $mode)""".update

  def removeSubscription(
      user_id: UUID,
      repository_id: UUID
  ): doobie.Update0 =
    sql"""delete from subscriptions where user_id=$user_id and repository_id=$repository_id""".update

  def checkSubscription(user_id: UUID, repository_id: UUID): Query0[Int] =
    sql"""select count(*) from subscriptions where user_id=$user_id and repository_id=$repository_id"""
      .query[Int]

  def getSubscribedUsers(repository_id: UUID): Query0[User] =
    sql"""SELECT u.slack_user_id, u.slack_channel_id, u.user_id FROM users u, subscriptions s
         WHERE s.repository_id = $repository_id AND s.user_id = u.user_id""".query[User]
}
