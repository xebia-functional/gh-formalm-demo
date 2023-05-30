import cats.effect.IO
import cats.effect.unsafe.implicits.global
import datatypes.KnownErrors.RepoNotFound
import datatypes.{Repository, RepositoryList}
import github.GitHubService
import org.http4s.Uri
import org.http4s.client._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import stubs.TestData._

import java.util.concurrent.{ExecutorService, Executors}

class GitHubServiceSpec extends AnyFlatSpec with should.Matchers {

  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO]        = JavaNetClientBuilder[IO].create

  val gitHubApiAlg: GitHubService[IO] =
    GitHubService.impl[IO](httpClient, Uri.unsafeFromString("https://api.github.com"))

  "Check valid list of repositories" should "be successful" in {
    gitHubApiAlg
      .checkIfReposExist(dummyValidRepositories)
      .unsafeRunSync() should be(dummyValidRepositories.subscriptions)
  }

  "Check empty list of repositories" should "return an empty list" in {
    gitHubApiAlg
      .checkIfReposExist(RepositoryList(List[Repository]()))
      .unsafeRunSync() should be(List[Repository]())
  }

  "Check invalid list of repositories" should "throw RepoNotFound exception" in {
    assertThrows[RepoNotFound] {
      gitHubApiAlg
        .checkValidRepo(dummyInvalidRepositories.subscriptions.head)
        .unsafeRunSync()
    }
  }
}
