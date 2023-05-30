package database

import doobie.{Query0, Update0}
import doobie.implicits._
import doobie.postgres.implicits._

import java.util.UUID

object Queries {
  def getSubscriptions(organization: String, repository: String): Query0[Int] = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""SELECT subscriptions FROM repositories WHERE repository_id=$repository_id""".query[Int]
  }

  def getWebhookId(organization: String, repository: String): Query0[Int] = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""SELECT webhook_id FROM repositories WHERE repository_id=$repository_id""".query[Int]
  }

  def insertRepository(organization: String, repository: String, webhook_id: Int): Update0 = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""INSERT INTO repositories (repository_id, organization, repository, subscriptions, webhook_id) 
         VALUES ($repository_id, $organization, $repository, 1, $webhook_id)""".update
  }

  def deleteRepository(organization: String, repository: String): Update0 = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""DELETE FROM repositories WHERE repository_id=$repository_id""".update
  }

  def incrementSubscription(organization: String, repository: String): Update0 = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""UPDATE repositories SET subscriptions = subscriptions + 1 WHERE repository_id=$repository_id""".update
  }

  def decrementSubscription(organization: String, repository: String): Update0 = {
    val repository_id = UUID.nameUUIDFromBytes((organization + repository).getBytes)
    sql"""UPDATE repositories SET subscriptions = subscriptions - 1 WHERE repository_id=$repository_id""".update
  }
}
