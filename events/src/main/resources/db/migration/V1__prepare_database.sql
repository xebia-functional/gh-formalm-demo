CREATE TABLE repositories
(
    repository_id UUID        NOT NULL,
    organization  VARCHAR(39) NOT NULL,
    repository    VARCHAR(39) NOT NULL,
    subscriptions INTEGER     NOT NULL,
    webhook_id    INTEGER     NOT NULL,
    PRIMARY KEY (repository_id)
);
