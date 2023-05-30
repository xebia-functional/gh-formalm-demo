import api.{Middleware, Routes}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import github.GitHubService
import io.circe.syntax.EncoderOps
import kafka.Streaming
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.Subscriptions
import stubs.TestData._
import stubs._

import java.util.UUID

class RoutesSpec extends AnyFlatSpec with should.Matchers {

  val subscriptionAlg: Subscriptions[IO] = SubscriptionsStub.impl[IO](dummyUser, dummySubscriptions)
  val githubAlg: GitHubService[IO]       = GitHubAPIStub.impl[IO]
  val streamingAlg: Streaming[IO]        = StreamingStub.impl[IO]
  val logger: Logger[IO]                 = Slf4jLogger.getLogger[IO]
  val middleware: Middleware[IO]         = new Middleware[IO](logger)

  "GET /subscription/<user>" should "return 200" in {
    val request = Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"/subscription/${dummyUser.user_id}")
    )

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.Ok)
  }

  "GET /subscription/<user>" should "return 404" in {
    val request = Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"/subscription/${UUID.randomUUID()}")
    )

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.NotFound)
  }

  "POST /subscription/<user>" should "return 201" in {
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"/subscription/${dummyUser.user_id}")
    ).withEntity(dummyRepositories.asJson)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.Ok)
  }

  "POST /subscription/<user>" should "return 400" in {
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"/subscription/${dummyUser.user_id}")
    ).withEntity("".asJson)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.BadRequest)
  }

  "DELETE /subscription/<user>" should "return 204" in {
    val request = Request[IO](
      method = Method.DELETE,
      uri = Uri.unsafeFromString(s"/subscription/${dummyUser.user_id}")
    ).withEntity(dummyRepositories.asJson)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.NoContent)
  }

  "DELETE /subscription/<user>" should "return 400" in {
    val request = Request[IO](
      method = Method.DELETE,
      uri = Uri.unsafeFromString(s"/subscription/${dummyUser.user_id}")
    ).withEntity("".asJson)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.BadRequest)
  }

  "DELETE /subscription/<user>" should "return 404" in {
    val request = Request[IO](
      method = Method.DELETE,
      uri = Uri.unsafeFromString(s"/subscription/${UUID.randomUUID()}")
    ).withEntity(dummyRepositories.asJson)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.NotFound)
  }

  "POST /subscription/slack/command/" should "return 202" in {
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"/subscription/slack/command")
    ).withEntity(dummySlackCommand)

    val response =
      new Routes[IO](subscriptionAlg, githubAlg, streamingAlg)(middleware).routes
        .orNotFound(request)
        .unsafeRunSync()

    response.status should be(Status.Accepted)
  }
}
