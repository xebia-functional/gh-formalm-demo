# Github Alerts Scala

![CI](https://github.com/47deg/github-alerts-scala-david.corral/actions/workflows/ci.yml/badge.svg)

This project aims to create a Slack bot that notifies users regarding alerts of certain GitHub repositories that they
have subscribed to previously.

## Structure

The project is divided into three main services/modules.

- `subscriptions-service` is the main service. It is in charge of managing the subscriptions and generating
  notifications.
    - On the one hand, it is the service receiving the Slack commands. When a subscription is created or removed, a
      Kafka message is generated and sent to the `events-service`.
    - On the other hand, it is the service receiving the events that are being managed at the `events-service` and
      generate a notification for each affected user. These notifications will be notified thanks to
      the `notifications-service`.

- `events-service` is in charge of managing the current actives `Github Webhooks` on the subscribed repos and of
  handling the events that are being received from the webhooks.
    - On the one hand, when a new subscription arrives, this service will create the webhook if it didn't exist yet. If
      an unsubscribe command arrives, this service will remove the webhook if it was the last subscribed user.
    - On the other hand, every time an event occurs in one of the repos with active webhooks, this service will receive
      it and will generate a Kafka message with the information of the event, which is going to be managed by the second
      part of the `subscriptions-service`.

- `notifications-service` is in charge of receiving the notifications that are being generated in
  the `subscriptions-service`. Each of these notifications contains the message of the event itself, and the user to be
  notified, so using the `Slack API` we directly send the message to the affected user.

## How to use it

### Prerequisites

- This repo is prepared to be run using [Docker](https://docs.docker.com/get-docker/).
- The project is designed to react to Slack commands to subscribe and unsubscribe, but you can simulate these Slack
  commands sending `HTTP POST` requests directly to `/api/subscriptions/slack/command`.
- In order to create the Slack bot, please refer to
  the [Slack API docs](https://slack.com/help/articles/115005265703-Create-a-bot-for-your-workspace). Once you have it,
  you will have to configure the [Slash Commands](https://api.slack.com/interactivity/slash-commands) section in your
  app. In this project we have modeled two main commands:
    - `/subscribe [organization]/[repository]` == `/subscribe DavidCorral94/github-alerts-dummy` to subscribe to a
      repository.
    - `/unsubscribe [organization]/[repository]` == `/unubscribe DavidCorral94/github-alerts-dummy` to unsubscribe from
      a repository.

### Considerations

- You have to bear in mind that you can only create `GitHub Webhooks` on repositories that you own or manage, that is
  the reason I created `DavidCorral94/github-alerts-dummy`.

### Steps

1. Clone the repo
2. Run `sbt assembly` to compile and generate the 'fat' `jar` files for each of the modules
3. Set your environment variables values in the file `.env`
    - `HOST` is the `url` in which the `events` service is running. This service is running in port `8079`
      locally, but in order to expose it, I would recommend using [ngrok](https://ngrok.com/)
    - `GITHUB_TOKEN_API` is the API token used to create the webhooks.
    - `SLACK_TOKEN_API` is the API token of your own Slack Bot
4. If you want to communicate from Slack command instead of sending `HTTP POST` request, you will have to run ngrok also for your `subscriptions` service (port `8080`) and configure that `url` in the [Slash Commands](https://api.slack.com/interactivity/slash-commands) section.
5. Run `docker compose --env-file .env up -d --build` to start the whole architecture

At this point (_if everything is well-configured_), your project is ready to be used. Go to Slack, open a private
conversation with your bot, write the `/subscribe` command and do some tests.
