package datatypes

import cats.effect.Sync
import datatypes.KnownErrors.UUIDNotValid
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID
import scala.util.Try

final case class User private (
    slack_user_id: String,
    slack_channel_id: String,
    user_id: UUID
)

object User {
  def apply(slack_user_id: String, slack_channel_id: String): User =
    User(
      slack_user_id,
      slack_channel_id,
      UUID.nameUUIDFromBytes(slack_user_id.getBytes())
    )

  def parseOrRaise[F[_]: Sync](user_id: String): F[UUID] = {
    val uuid = Try(UUID.fromString(user_id))
    if (uuid.isSuccess)
      Sync[F].pure(uuid.get)
    else
      Sync[F].raiseError(UUIDNotValid(user_id))
  }

  implicit val userDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val userEncoder: Encoder[User] =
    deriveEncoder[User]
}
