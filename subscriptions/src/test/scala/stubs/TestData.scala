package stubs

import datatypes._
import org.http4s.UrlForm

import java.time.LocalDateTime

object TestData {

  val dummyUser: User = User("U021VC19RT8", "D021Z35C1D3")

  val dummyRepositories: RepositoryList = RepositoryList(
    List(
      Repository("47deg", "scalacon21-slides"),
      Repository("47deg", "47deg.github.io")
    )
  )

  val dummySubscriptions: SubscriptionList[Subscription] =
    new SubscriptionList(
      List(
        Subscription(
          Repository("47deg", "scalacon21-slides"),
          LocalDateTime.now()
        ),
        Subscription(
          Repository("47deg", "47deg.github.io"),
          LocalDateTime.now()
        )
      )
    )

  val dummySlackCommand: UrlForm = UrlForm(
    "command"      -> "/subscribe",
    "text"         -> "47deg/scalacon21-slides",
    "response_url" -> "https://hooks.slack.com/commands/T024G6JTP/2182982874551/NEa5zJt14LuQaUwvrLeujAQH",
    "trigger_id"   -> "2190958771078.2152222941.c9f82c13c8f2323b328f5a2c8e821579",
    "user_id"      -> "T017DHDS1B8",
    "channel_id"   -> "L0SJSDF71FC"
  )

  val dummyValidRepositories: RepositoryList = RepositoryList(
    List(
      Repository("47deg", "scalacon21-slides"),
      Repository("47deg", "47deg.github.io")
    )
  )

  val dummyInvalidRepositories: RepositoryList = RepositoryList(
    List(Repository("47deg", "non-existing-repo"))
  )

  val dummyMixedRepositories: RepositoryList = RepositoryList(
    List(
      Repository("47deg", "scalacon21-slides"),
      Repository("47deg", "non-existing-repo")
    )
  )

  val dummyNonExistingRepositories: RepositoryList = RepositoryList(
    List(
      Repository("47deg", "no-exists"),
      Repository("47deg", "non-existing-repo")
    )
  )

  val dummySC: SlackCommand = SlackCommand(
    "subscribe",
    dummyRepositories,
    "https://hooks.slack.com/commands/T024G6JTP/2182982874551/NEa5zJt14LuQaUwvrLeujAQH",
    "2190958771078.2152222941.c9f82c13c8f2323b328f5a2c8e821579",
    dummyUser
  )

  val dummyInvalidSlackCommand: SlackCommand = SlackCommand(
    "lol",
    dummyRepositories,
    "https://hooks.slack.com/commands/T024G6JTP/2182982874551/NEa5zJt14LuQaUwvrLeujAQH",
    "2190958771078.2152222941.c9f82c13c8f2323b328f5a2c8e821579",
    dummyUser
  )

}
