import cats.effect.IO
import cats.effect.unsafe.implicits.global
import datatypes._
import org.http4s.client._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.Slack

class SlackSpec extends AnyFlatSpec with should.Matchers {

  val httpClient: Client[IO]     = JavaNetClientBuilder[IO].create
  val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]
  val slackTokenAPI: String      = sys.env.getOrElse("SLACK_TOKEN_API", "")
  val slackMessage: SlackMessage = SlackMessage("U021VC19RT8", "Test message")
  val slackAlg: Slack[IO]        = Slack.impl[IO](logger, httpClient, slackTokenAPI)

  "Send message to user with Slack API" should "be successful" in {
    slackAlg.sendMessage(slackMessage).unsafeRunSync()
  }
}
