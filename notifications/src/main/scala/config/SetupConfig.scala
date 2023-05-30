package config

import cats.effect.Sync
import pureconfig._
import pureconfig.generic.auto._

object SetupConfig {
  def loadConfig[F[_]: Sync]: F[ServiceConfig] =
    Sync[F].delay(ConfigSource.default.loadOrThrow[ServiceConfig])
}
