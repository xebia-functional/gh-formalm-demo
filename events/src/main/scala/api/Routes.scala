package api

import cats.effect.Async
import cats.implicits._
import datatypes.{Event, ZenEvent}
import kafka.Streaming
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.Http4sDsl

final class Routes[F[_]: Async](streaming: Streaming[F], topic: String)(m: Middleware[F])
    extends Http4sDsl[F] {

  private val generateEventMessage: Event => F[Unit] = {
    case _: ZenEvent => Async[F].unit
    case e           => streaming.sendMessage(topic, e.generateEventMessage)
  }

  private def _routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "webhook" => (req.as[Event] >>= generateEventMessage) >> Ok()
    }

  def routes: HttpRoutes[F] = m(_routes)
}
