package stubs

import cats.Applicative
import datatypes.{EventMessage, KafkaMessage}
import fs2.Stream
import kafka.Streaming

object StreamingStub {
  def impl[F[_]: Applicative]: Streaming[F] =
    new Streaming[F] {
      def sendMessage(topic: String, message: KafkaMessage): F[Unit] = Applicative[F].unit
      def consumeMessages(inputTopic: String, sinkTopic: String): Stream[F, EventMessage] =
        Stream.eval(Applicative[F].pure(EventMessage("", "", "")))
    }
}
