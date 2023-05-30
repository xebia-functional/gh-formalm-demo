package datatypes

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait Event {
  val organization: String
  val repository: String

  def generateEventMessage: EventMessage
}

object Event {
  implicit val eventDecoder: Decoder[Event] = (c: HCursor) =>
    if (c.downField("zen").succeeded)
      ZenEvent.zenEventDecoder(c)
    else if (c.downField("master_branch").succeeded)
      CreateEvent.createEventDecoder(c)
    else if (c.downField("ref_type").succeeded)
      DeleteEvent.deleteEventDecoder(c)
    else if (c.downField("comment").succeeded)
      IssueCommentEvent.issueCommentEventDecoder(c)
    else if (c.downField("issue").succeeded)
      IssueEvent.issueEventDecoder(c)
    else if (c.downField("member").succeeded)
      MemberEvent.memberEventDecoder(c)
    else if (c.downField("pull_request").succeeded)
      PullRequestEvent.pullRequestEventDecoder(c)
    else
      PushEvent.pushEventDecoder(c)

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
}

final case class ZenEvent(zen: String, sender: String, organization: String, repository: String)
    extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"Webhook created for $organization/$repository",
      organization,
      repository
    )
}

object ZenEvent {
  implicit def zenEventDecoder: Decoder[ZenEvent] =
    (c: HCursor) =>
      for {
        zen    <- c.downField("zen").as[String]
        sender <- c.downField("sender").downField("login").as[String]
        repo   <- c.downField("repository").downField("full_name").as[Repository]
      } yield ZenEvent(zen, sender, repo.organization, repo.repository)
}

final case class CreateEvent(
    ref: String,
    category: String,
    description: String,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"A new $category named '$ref' has been created at '$organization/$repository' by @$sender",
      organization,
      repository
    )
}

object CreateEvent {
  implicit def createEventDecoder: Decoder[CreateEvent] =
    (c: HCursor) =>
      for {
        ref         <- c.downField("ref").as[String]
        category    <- c.downField("ref_type").as[String]
        description <- c.downField("description").as[String]
        sender      <- c.downField("sender").downField("login").as[String]
        repo        <- c.downField("repository").downField("full_name").as[Repository]
      } yield CreateEvent(
        ref,
        category,
        description,
        sender,
        repo.organization,
        repo.repository
      )
}

final case class DeleteEvent(
    ref: String,
    category: String,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"The $category named '$ref' has been removed at '$organization/$repository' by @$sender",
      organization,
      repository
    )
}

object DeleteEvent {
  implicit def deleteEventDecoder: Decoder[DeleteEvent] =
    (c: HCursor) =>
      for {
        ref      <- c.downField("ref").as[String]
        category <- c.downField("ref_type").as[String]
        sender   <- c.downField("sender").downField("login").as[String]
        repo     <- c.downField("repository").downField("full_name").as[Repository]
      } yield DeleteEvent(ref, category, sender, repo.organization, repo.repository)
}

final case class Issue(id: Int, title: String, html_url: String)

object Issue {
  implicit def issueDecoder: Decoder[Issue] = deriveDecoder[Issue]
  implicit def issueEncoder: Encoder[Issue] = deriveEncoder[Issue]
}

final case class IssueEvent(
    action: String,
    issue: Issue,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"The issue named '${issue.title}' has been $action at '$organization/$repository' by @$sender: ${issue.html_url}",
      organization,
      repository
    )
}

object IssueEvent {
  implicit def issueEventDecoder: Decoder[IssueEvent] =
    (c: HCursor) =>
      for {
        action <- c.downField("action").as[String]
        issue  <- c.downField("issue").as[Issue]
        sender <- c.downField("sender").downField("login").as[String]
        repo   <- c.downField("repository").downField("full_name").as[Repository]
      } yield IssueEvent(action, issue, sender, repo.organization, repo.repository)
}

final case class IssueComment(id: Int, html_url: String)

object IssueComment {
  implicit def issueDecoder: Decoder[IssueComment] = deriveDecoder[IssueComment]
  implicit def issueEncoder: Encoder[IssueComment] = deriveEncoder[IssueComment]
}

final case class IssueCommentEvent(
    action: String,
    issue: Issue,
    comment: IssueComment,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"A comment in the issue named '${issue.title}' has been $action at '$organization/$repository' by @$sender: ${comment.html_url}",
      organization,
      repository
    )
}

object IssueCommentEvent {
  implicit def issueCommentEventDecoder: Decoder[IssueCommentEvent] =
    (c: HCursor) =>
      for {
        action  <- c.downField("action").as[String]
        issue   <- c.downField("issue").as[Issue]
        comment <- c.downField("comment").as[IssueComment]
        sender  <- c.downField("sender").downField("login").as[String]
        repo    <- c.downField("repository").downField("full_name").as[Repository]
      } yield IssueCommentEvent(
        action,
        issue,
        comment,
        sender,
        repo.organization,
        repo.repository
      )
}

final case class Member(id: Int, login: String, html_url: String)

object Member {
  implicit def memberDecoder: Decoder[Member] = deriveDecoder[Member]
  implicit def memberEncoder: Encoder[Member] = deriveEncoder[Member]
}

final case class MemberEvent(
    action: String,
    member: Member,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"The user named '${member.login}' has been $action at '$organization/$repository' by @$sender",
      organization,
      repository
    )
}

object MemberEvent {
  implicit def memberEventDecoder: Decoder[MemberEvent] =
    (c: HCursor) =>
      for {
        action <- c.downField("action").as[String]
        member <- c.downField("member").as[Member]
        sender <- c.downField("sender").downField("login").as[String]
        repo   <- c.downField("repository").downField("full_name").as[Repository]
      } yield MemberEvent(action, member, sender, repo.organization, repo.repository)
}

final case class PullRequest(id: Int, title: String, state: String, html_url: String)

object PullRequest {
  implicit def pullRequestDecoder: Decoder[PullRequest] = deriveDecoder[PullRequest]
  implicit def pullRequestEncoder: Encoder[PullRequest] = deriveEncoder[PullRequest]
}

final case class PullRequestEvent(
    action: String,
    number: Int,
    pull_request: PullRequest,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"The pull request '${pull_request.title}' has been $action at '$organization/$repository' by @$sender: ${pull_request.html_url}",
      organization,
      repository
    )
}

object PullRequestEvent {
  implicit def pullRequestEventDecoder: Decoder[PullRequestEvent] =
    (c: HCursor) =>
      for {
        action      <- c.downField("action").as[String]
        number      <- c.downField("number").as[Int]
        pullRequest <- c.downField("pull_request").as[PullRequest]
        sender      <- c.downField("sender").downField("login").as[String]
        repo        <- c.downField("repository").downField("full_name").as[Repository]
      } yield PullRequestEvent(
        action,
        number,
        pullRequest,
        sender,
        repo.organization,
        repo.repository
      )
}

final case class PushEvent(
    ref: String,
    sender: String,
    organization: String,
    repository: String
) extends Event {
  def generateEventMessage: EventMessage =
    EventMessage.apply(
      s"One or more commits have been pushed to '$ref' at '$organization/$repository' by @$sender",
      organization,
      repository
    )
}

object PushEvent {
  implicit def pushEventDecoder: Decoder[PushEvent] =
    (c: HCursor) =>
      for {
        ref    <- c.downField("ref").as[String]
        sender <- c.downField("sender").downField("login").as[String]
        repo   <- c.downField("repository").downField("full_name").as[Repository]
      } yield PushEvent(ref, sender, repo.organization, repo.repository)
}
