import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import database.DatabaseService
import doobie.Transactor
import doobie.implicits.{toSqlInterpolator, _}
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.TestData.ownedRepo

class DatabaseServiceSpec
    extends AnyFlatSpec
    with should.Matchers
    with ForAllTestContainer
    with BeforeAndAfterEach {

  override val container: PostgreSQLContainer = PostgreSQLContainer()

  override def afterStart(): Unit = {
    val flyway: Flyway =
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
    ()
  }

  override def beforeEach(): Unit =
    sql"""TRUNCATE repositories""".update.run.transact(xa).void.unsafeRunSync()

  lazy val xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password
    )

  def databaseServiceAlg: DatabaseService[IO] = DatabaseService.impl[IO](xa)

  "Add repository webhook and check it" should "be successful" in {
    databaseServiceAlg
      .addRepository(ownedRepo.organization, ownedRepo.repository, 47)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .getWebhookId(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(Some(47))
  }

  "Add repository webhook, increment subscriptions and check them" should "be successful" in {
    databaseServiceAlg
      .addRepository(ownedRepo.organization, ownedRepo.repository, 47)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .incrementSubscription(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .checkActiveSubscriptions(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(Some(2))
  }

  "Add repository webhook, decrement subscription, remove it and check it" should "be successful" in {
    databaseServiceAlg
      .addRepository(ownedRepo.organization, ownedRepo.repository, 47)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .decrementSubscription(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .removeRepository(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(1)

    databaseServiceAlg
      .getWebhookId(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(None)
  }

  "Check webhook id of non-inserted repo" should "be return None" in {
    databaseServiceAlg
      .getWebhookId(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(None)
  }

  "Check active subscriptions of non-inserted repo" should "be return None" in {
    databaseServiceAlg
      .checkActiveSubscriptions(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync() should be(None)
  }

  "Insert duplicated repo webhook" should "throw PSQLException exception" in {
    databaseServiceAlg
      .addRepository(ownedRepo.organization, ownedRepo.repository, 47)
      .unsafeRunSync() should be(1)

    assertThrows[PSQLException] {
      databaseServiceAlg
        .addRepository(ownedRepo.organization, ownedRepo.repository, 47)
        .unsafeRunSync()
    }
  }

  container.stop()
}
