package github

import cats.Applicative
import cats.effect.Async
import cats.implicits._
import datatypes.KnownErrors.RepoNotFound
import datatypes.{Repository, RepositoryList}
import org.http4s.Uri
import org.http4s.client.Client

trait GitHubService[F[_]] {
  def checkIfReposExist(
      repositoryList: RepositoryList
  )(implicit e: Applicative[F]): F[List[Repository]] =
    repositoryList.subscriptions.traverse(checkValidRepo)
  def checkValidRepo(r: Repository): F[Repository]
}

object GitHubService {

  def apply[F[_]](implicit ev: GitHubService[F]): GitHubService[F] = ev

  def impl[F[_]: Async](c: Client[F], uri: Uri): GitHubService[F] =
    new GitHubService[F] {
      def checkValidRepo(r: Repository): F[Repository] =
        c.statusFromUri(uri = uri / "repos" / r.organization / r.repository)
          .ensure(RepoNotFound(r))(_.isSuccess)
          .as(r)
    }
}
