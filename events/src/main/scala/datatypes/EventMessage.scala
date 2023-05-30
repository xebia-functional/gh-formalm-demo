package datatypes

import cats.implicits.catsSyntaxTuple4Semigroupal
import vulcan.Codec

import java.util.UUID

final case class EventMessage(
    repoId: UUID,
    message: String,
    organization: String,
    repository: String
)

object EventMessage {
  def apply(message: String, organization: String, repository: String): EventMessage =
    new EventMessage(
      UUID.nameUUIDFromBytes((organization + repository).getBytes),
      message,
      organization,
      repository
    )

  implicit val eventMessageCodec: Codec[EventMessage] =
    Codec.record(
      name = "EventMessage",
      namespace = "com.github.alerts"
    ) { field =>
      (
        field("repoId", _.repoId),
        field("message", _.message),
        field("organization", _.organization),
        field("repository", _.repository)
      ).mapN(EventMessage(_, _, _, _))
    }
}
