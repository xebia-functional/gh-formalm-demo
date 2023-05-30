package stubs

import cats.Applicative
import datatypes.Repository
import github.GitHubService

object GitHubAPIStub {
  def impl[F[_]: Applicative]: GitHubService[F] =
    new GitHubService[F] {
      def checkValidRepo(r: Repository): F[Repository] =
        Applicative[F].pure(r)
    }
}
