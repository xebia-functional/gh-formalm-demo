package config

case class ServiceConfig(
    streaming: StreamingConfig,
    slackTokenAPI: String
)

case class StreamingConfig(
    bootstrapServers: String,
    schemaRegistryUri: String,
    consumerId: String,
    maxConcurrent: Int,
    requiredInputTopics: List[String]
)
