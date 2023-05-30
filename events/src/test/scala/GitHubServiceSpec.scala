import cats.effect.IO
import cats.effect.unsafe.implicits.global
import datatypes.KnownErrors.WebhookError
import datatypes._
import github.GitHubService
import org.http4s.client._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.TestData._

import java.util.concurrent.{ExecutorService, Executors}

class GitHubServiceSpec extends AnyFlatSpec with should.Matchers {

  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO]        = JavaNetClientBuilder[IO].create
  val githubTokenAPI: String        = sys.env.getOrElse("GITHUB_TOKEN_API", "")
  val host: String                  = sys.env.getOrElse("HOST", "")

  val gitHubApiAlg: GitHubService[IO] = GitHubService.impl[IO](httpClient, githubTokenAPI, host)

  "Create and remove webhook for owned repository" should "be successful" in {
    if (githubTokenAPI.equalsIgnoreCase(""))
      cancel("The GITHUB_TOKEN_API env variable is required")
    if (host.equalsIgnoreCase(""))
      cancel("The HOST env variable is required")

    val webhookResponse = gitHubApiAlg
      .createWebhook(ownedRepo.organization, ownedRepo.repository)
      .unsafeRunSync()
    webhookResponse shouldBe a[WebhookResponse]
    webhookResponse.id should be > 0

    gitHubApiAlg
      .removeWebhook(ownedRepo.organization, ownedRepo.repository, webhookResponse.id)
      .unsafeRunSync() should be(true)
  }

  "Create webhook for not owned repository" should "throw WebhookError" in {
    assertThrows[WebhookError] {
      gitHubApiAlg
        .createWebhook(notOwnedRepo.organization, notOwnedRepo.repository)
        .unsafeRunSync()
    }
  }

  "Remove webhook for not owned repository" should "throw WebhookError" in {
    assertThrows[WebhookError] {
      gitHubApiAlg
        .removeWebhook(notOwnedRepo.organization, notOwnedRepo.repository, 9999)
        .unsafeRunSync()
    }
  }

}
