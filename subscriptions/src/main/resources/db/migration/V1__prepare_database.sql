CREATE TABLE IF NOT EXISTS users
(
    user_id          UUID        NOT NULL,
    slack_user_id    VARCHAR(20) NOT NULL,
    slack_channel_id VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS repositories
(
    repository_id UUID        NOT NULL,
    organization  VARCHAR(39) NOT NULL,
    repository    VARCHAR(39) NOT NULL,
    PRIMARY KEY (repository_id)
);

CREATE TABLE IF NOT EXISTS subscriptions
(
    user_id       UUID      NOT NULL,
    repository_id UUID      NOT NULL,
    subscribed_at TIMESTAMP NOT NULL,
    mode          SMALLINT  NOT NULL,
    PRIMARY KEY (user_id, repository_id),
    CONSTRAINT fk_users
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),
    CONSTRAINT fk_repositories
        FOREIGN KEY (repository_id)
            REFERENCES repositories (repository_id)
);