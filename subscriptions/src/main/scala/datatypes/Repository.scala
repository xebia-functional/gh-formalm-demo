package datatypes

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID

final case class Repository(
    organization: String,
    repository: String
) {
  def getUUID: UUID =
    UUID.nameUUIDFromBytes((organization + repository).getBytes)

  override def toString: String = organization + "/" + repository
}

object Repository {
  implicit val repositoryDecoder: Decoder[Repository] =
    deriveDecoder[Repository]
  implicit val repositoryEncoder: Encoder[Repository] =
    deriveEncoder[Repository]
}
