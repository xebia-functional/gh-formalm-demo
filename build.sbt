val Http4sVersion              = "0.23.1"
val CirceVersion               = "0.14.1"
val MunitVersion               = "0.7.28"
val LogbackVersion             = "1.2.5"
val MunitCatsEffectVersion     = "1.0.5"
val SvmVersion                 = "20.2.0"
val DoobieVersion              = "1.0.0-M5"
val FlywayVersion              = "7.10.0"
val Fs2KafkaVersion            = "2.2.0"
val VulkanVersion              = "1.7.1"
val Log4CatsVersion            = "2.1.1"
val PureConfigVersion          = "0.16.0"
val ScalaTestVersion           = "3.2.9"
val TestContainersVersion      = "0.39.6"
val EmbeddedKafkaSchemaVersion = "6.2.0"

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.1.5" // necessary atm

val coreDependencies = Seq(
  "org.http4s"              %% "http4s-blaze-server"             % Http4sVersion,
  "org.http4s"              %% "http4s-blaze-client"             % Http4sVersion,
  "org.http4s"              %% "http4s-circe"                    % Http4sVersion,
  "org.http4s"              %% "http4s-dsl"                      % Http4sVersion,
  "io.circe"                %% "circe-generic"                   % CirceVersion,
  "org.scalameta"           %% "munit"                           % MunitVersion               % Test,
  "org.typelevel"           %% "munit-cats-effect-3"             % MunitCatsEffectVersion     % Test,
  "ch.qos.logback"           % "logback-classic"                 % LogbackVersion,
  "org.scalameta"           %% "svm-subs"                        % SvmVersion,
  "org.tpolecat"            %% "doobie-core"                     % DoobieVersion,
  "org.tpolecat"            %% "doobie-h2"                       % DoobieVersion,
  "org.tpolecat"            %% "doobie-hikari"                   % DoobieVersion,
  "org.tpolecat"            %% "doobie-postgres"                 % DoobieVersion,
  "org.flywaydb"             % "flyway-core"                     % FlywayVersion,
  "com.github.fd4s"         %% "fs2-kafka"                       % Fs2KafkaVersion,
  "com.github.fd4s"         %% "fs2-kafka-vulcan"                % Fs2KafkaVersion,
  "com.github.fd4s"         %% "vulcan-enumeratum"               % VulkanVersion,
  "org.typelevel"            % "log4cats-core_2.13"              % Log4CatsVersion,
  "org.typelevel"           %% "log4cats-slf4j"                  % Log4CatsVersion,
  "com.github.pureconfig"   %% "pureconfig"                      % PureConfigVersion,
  "org.scalatest"           %% "scalatest"                       % ScalaTestVersion           % Test,
  "com.dimafeng"            %% "testcontainers-scala-scalatest"  % TestContainersVersion      % Test,
  "com.dimafeng"            %% "testcontainers-scala-postgresql" % TestContainersVersion      % Test,
  "io.github.embeddedkafka" %% "embedded-kafka-schema-registry"  % EmbeddedKafkaSchemaVersion % Test
)

lazy val subscriptions = (project in file("subscriptions"))
  .settings(
    scalafmtOnCompile := true,
    assembly / mainClass := Some("Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")       => MergeStrategy.discard
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    },
    organization := "com.47deg",
    name := "github-alerts-scala-subscriptions-david.corral",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    scalacOptions += "-Xfatal-warnings",
    semanticdbEnabled := true, // necessary atm
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    ),
    libraryDependencies ++= coreDependencies,
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val events = (project in file("events"))
  .settings(
    scalafmtOnCompile := true,
    assembly / mainClass := Some("Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")       => MergeStrategy.discard
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    },
    organization := "com.47deg",
    name := "github-alerts-scala-events-david.corral",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    scalacOptions += "-Xfatal-warnings",
    semanticdbEnabled := true, // necessary atm
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    ),
    libraryDependencies ++= coreDependencies,
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val notifications = (project in file("notifications"))
  .settings(
    scalafmtOnCompile := true,
    assembly / mainClass := Some("Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")       => MergeStrategy.discard
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    },
    organization := "com.47deg",
    name := "github-alerts-scala-notifications-david.corral",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    scalacOptions += "-Xfatal-warnings",
    semanticdbEnabled := true, // necessary atm
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    ),
    libraryDependencies ++= coreDependencies,
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
