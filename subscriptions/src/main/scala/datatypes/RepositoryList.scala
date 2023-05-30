package datatypes

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RepositoryList(subscriptions: List[Repository]) extends AnyVal
object RepositoryList {
  implicit def repositoryListDecoder: Decoder[RepositoryList] = deriveDecoder
  implicit def repositoryListEncoder: Encoder[RepositoryList] = deriveEncoder
}
