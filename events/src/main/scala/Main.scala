import api.Server
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = Server.serve[IO].compile.drain.as(ExitCode.Success)
}
