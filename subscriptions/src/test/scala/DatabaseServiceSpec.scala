import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import database.DatabaseService
import datatypes._
import doobie.Transactor
import doobie.util.invariant.InvariantViolation
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.TestData._

import java.util.UUID

class DatabaseServiceSpec extends AnyFlatSpec with should.Matchers with ForAllTestContainer {

  override val container: PostgreSQLContainer = PostgreSQLContainer()

  def xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password
    )

  "Perform Flyway migrations" should "be ok" in {
    def flyway: Flyway =
      Flyway.configure
        .mixed(true)
        .baselineOnMigrate(true)
        .dataSource(
          container.jdbcUrl,
          container.username,
          container.password
        )
        .load

    flyway.clean()
    flyway.migrate()
  }

  "Check there is no subscriptions" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .getSubscriptions(dummyUser.user_id)
      .unsafeRunSync()
      .subscriptions should be(List())
  }

  "Add new user" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .addUser(dummyUser)
      .unsafeRunSync() should be(true)
  }

  "Get existing user" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .getUser(dummyUser.user_id)
      .unsafeRunSync() should be(dummyUser)
  }

  "Get non-existing user" should "throw InvariantViolation exception" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    assertThrows[InvariantViolation] {
      databaseServiceAlg
        .getUser(UUID.randomUUID())
        .unsafeRunSync()
    }
  }

  "Check if exists user" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .checkUser(dummyUser.user_id)
      .unsafeRunSync() should be(dummyUser.user_id)

  }

  "Add first repository" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .addRepository(dummyRepositories.subscriptions.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.head)
  }

  "Add second repository" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .addRepository(dummyRepositories.subscriptions.tail.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.tail.head)
  }

  "Add first subscription" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .addSubscription(dummyUser.user_id, dummyRepositories.subscriptions.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.head)
  }

  "Add second subscription" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .addSubscription(dummyUser.user_id, dummyRepositories.subscriptions.tail.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.tail.head)
  }

  "Add duplicated subscription" should "throw PSQLException exception" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    assertThrows[PSQLException] {
      databaseServiceAlg
        .addSubscription(dummyUser.user_id, dummyRepositories.subscriptions.head)
        .unsafeRunSync()
    }
  }

  "Get subscriptions" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    val subscriptions = databaseServiceAlg
      .getSubscriptions(dummyUser.user_id)
      .unsafeRunSync()

    subscriptions.subscriptions.size should be(2)
    subscriptions.subscriptions
      .forall(p => dummyRepositories.subscriptions.contains(p.repository)) should be(true)
  }

  "Get subscriptions of non-existing user" should "be return empty List" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .getSubscriptions(UUID.randomUUID())
      .unsafeRunSync() should be(SubscriptionList(List.empty))
  }

  "Remove subscription" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .removeSubscription(dummyUser.user_id, dummyRepositories.subscriptions.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.head)
  }

  "Remove non-existing subscription" should "be successful" in {
    val databaseServiceAlg = DatabaseService.impl[IO](xa)
    databaseServiceAlg
      .removeSubscription(UUID.randomUUID(), dummyRepositories.subscriptions.head)
      .unsafeRunSync() should be(dummyRepositories.subscriptions.head)
  }

  container.stop()
}
