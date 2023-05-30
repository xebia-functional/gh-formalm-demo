package datatypes

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder

import java.util.UUID

final case class Repository(organization: String, repository: String) {
  def getUUID: UUID = UUID.nameUUIDFromBytes((organization + repository).getBytes)
}

object Repository {
  implicit val repositoryDecoder: Decoder[Repository] = Decoder.decodeString.emap(value =>
    value.split("/") match {
      case Array(org, rep) => Right(Repository(org, rep))
      case _               => Left(s"The repo named '$value' is invalid")
    }
  )

  implicit val repositoryEncoder: Encoder[Repository] = deriveEncoder[Repository]
}
