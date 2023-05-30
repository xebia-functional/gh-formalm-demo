package datatypes

import cats.data.Chain
import cats.implicits.toFoldableOps
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class SlackCommand(
    command: String,
    repositories: RepositoryList,
    response_url: String,
    trigger_id: String,
    user: User
)

object SlackCommand {
  def detectRepos(text: String): RepositoryList =
    RepositoryList(
      text
        .split(" ")
        .toList
        .map(repoListText => {
          Repository(
            repoListText.split("/").toList.head,
            repoListText.split("/").toList(1)
          )
        })
    )

  def decodeSlackCommand(values: Map[String, Chain[String]]): SlackCommand = {
    SlackCommand(
      values("command").fold.replace("/", ""),
      detectRepos(values("text").fold.trim),
      values("response_url").fold,
      values("trigger_id").fold,
      User.apply(values("user_id").fold, values("channel_id").fold)
    )
  }

  implicit val slackCommandEncoder: Encoder[SlackCommand] =
    deriveEncoder[SlackCommand]
}
