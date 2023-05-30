package config

case class ServiceConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    streaming: StreamingConfig,
    github: GitHubConfig
)

case class GitHubConfig(baseUrl: String)

case class ServerConfig(host: String, port: Int)

case class DatabaseConfig(url: String, user: String, password: String)

case class StreamingConfig(
    bootstrapServers: String,
    schemaRegistryUri: String,
    producerId: String,
    consumerId: String,
    maxConcurrent: Int,
    requiredInputTopics: List[String],
    requiredOutputTopics: List[String]
)
