import cats.effect.IO
import cats.effect.unsafe.implicits.global
import database.DatabaseService
import datatypes.KnownErrors._
import datatypes.SlackResponse
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import services.Subscriptions
import stubs.DatabaseServiceStub
import stubs.TestData._

import java.util.UUID

class SubscriptionsSpec extends AnyFlatSpec with should.Matchers {

  val databaseServiceStub: DatabaseService[IO] =
    DatabaseServiceStub.impl[IO](dummyUser, dummySubscriptions)
  val subscriptionsAlg: Subscriptions[IO] = Subscriptions.impl[IO](databaseServiceStub)

  "Get subscriptions of existing user" should "return the subscriptions" in {
    subscriptionsAlg
      .getSubscriptions(dummyUser.user_id)
      .unsafeRunSync() should be(dummySubscriptions)
  }

  "Get subscriptions of non-existing user" should "throw UserNotFound exception" in {
    assertThrows[UserNotFound] {
      subscriptionsAlg.getSubscriptions(UUID.randomUUID()).unsafeRunSync()
    }
  }

  "Add subscriptions to existing user" should "return the subscriptions" in {
    subscriptionsAlg
      .postSubscriptions(dummyUser.user_id, dummyRepositories.subscriptions)
      .unsafeRunSync() should be(dummyRepositories.subscriptions)
  }

  "Add subscriptions to non-existing user" should "throw UserNotFound exception" in {
    assertThrows[UserNotFound] {
      subscriptionsAlg
        .postSubscriptions(UUID.randomUUID(), dummyRepositories.subscriptions)
        .unsafeRunSync() should be(dummyRepositories.subscriptions)
    }
  }

  "Delete existing subscriptions of existing user" should "return the a list of true" in {
    subscriptionsAlg
      .deleteSubscriptions(dummyUser.user_id, dummyRepositories.subscriptions)
      .unsafeRunSync() should be(dummyRepositories.subscriptions)
  }

  "Delete mixed-existing subscriptions of existing user" should "throw SubscriptionNotFound exception" in {
    assertThrows[SubscriptionNotFound.type] {
      subscriptionsAlg
        .deleteSubscriptions(
          dummyUser.user_id,
          dummyMixedRepositories.subscriptions
        )
        .unsafeRunSync()
    }
  }

  "Delete non-existing subscriptions of existing user" should "throw SubscriptionNotFound exception" in {
    assertThrows[SubscriptionNotFound.type] {
      subscriptionsAlg
        .deleteSubscriptions(
          dummyUser.user_id,
          dummyNonExistingRepositories.subscriptions
        )
        .unsafeRunSync()
    }
  }

  "Delete subscriptions of non-existing user" should "throw UserNotFound exception" in {
    assertThrows[UserNotFound] {
      subscriptionsAlg
        .deleteSubscriptions(UUID.randomUUID(), dummyRepositories.subscriptions)
        .unsafeRunSync() should be(List(true, true))
    }
  }

  "Add subscriptions using slack command" should "return the slack response" in {
    subscriptionsAlg
      .postSubscriptionsSlack(dummySC)
      .unsafeRunSync() should be(
      SlackResponse(
        "ephemeral",
        s"You have been subscribed to ${dummySC.repositories.subscriptions.mkString(", ")}!",
        List()
      )
    )
  }

  "Add subscriptions using an invalid slack command" should "throw CommandNotImplemented exception" in {
    assertThrows[CommandNotImplemented] {
      subscriptionsAlg
        .postSubscriptionsSlack(dummyInvalidSlackCommand)
        .unsafeRunSync()
    }
  }
}
