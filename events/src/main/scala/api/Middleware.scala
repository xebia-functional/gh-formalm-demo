package api

import cats.data.{Kleisli, OptionT}
import cats.effect.Async
import doobie.util.invariant.InvariantViolation
import org.http4s.{DecodeFailure, HttpRoutes, Response, Status}
import cats.implicits._
import datatypes.KnownErrors._
import io.circe.Json
import io.circe.syntax.KeyOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.typelevel.log4cats.Logger

import java.io.IOException
import java.sql.SQLException

class Middleware[F[_]: Async](logger: Logger[F]) {

  def genResponse(s: Status, msg: String): Response[F] =
    Response[F](s).withEntity(
      Json.obj(
        "error" := msg
      )
    )

  def badRequest(e: Throwable): F[Response[F]] =
    logger.error(e.getMessage).as(genResponse(Status.BadRequest, e.getMessage))

  def notFound(e: Throwable): F[Response[F]] =
    logger.warn(e.getMessage).as(genResponse(Status.NotFound, e.getMessage))

  def notImplemented(e: Throwable): F[Response[F]] =
    logger.warn(e.getMessage).as(genResponse(Status.NotImplemented, e.getMessage))

  def internalServerError(e: Throwable): F[Response[F]] =
    logger.error(e.getMessage).as(genResponse(Status.InternalServerError, e.getMessage))

  def acceptedWithErrors(e: Throwable): F[Response[F]] =
    logger
      .error(e.getMessage)
      .as(genResponse(Status.Accepted, s"Oops, something went wrong: ${e.getMessage}"))

  val handleError: KnownErrors => F[Response[F]] = {
    case e: WebhookError          => badRequest(e)
    case e: CommandNotImplemented => notImplemented(e)
    case e: InvalidRepoName       => badRequest(e)
  }

  def apply(httpRoutes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        httpRoutes.run(req).value.handleErrorWith {
          case e: KnownErrors        => handleError(e).map(_.some)
          case e: DecodeFailure      => badRequest(e).map(_.some)
          case _: InvariantViolation => notFound(new Throwable("User not found")).map(_.some)
          case e: SQLException       => internalServerError(e).map(_.some)
          case e: IOException =>
            internalServerError(new Throwable(s"IO Exception happened: ${e.getMessage}"))
              .map(_.some)
          case _ => internalServerError(new Throwable("Internal Server Error")).map(_.some)
        }
      }
    }
}
