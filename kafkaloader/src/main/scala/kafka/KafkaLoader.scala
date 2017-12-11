package kafka

import java.util.Properties

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.{Logger, LoggerFactory}

class KafkaLoader[T] {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  object futureCallback extends Callback {
    def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      Option(metadata) match {
        case Some(m) => logger.debug("got some great result")
        case None => ;
      }
      Option(exception) match {
        case Some(e) => logger.error(s"got an error sending a message to Kafka: ${e.getClass} - ${e.getLocalizedMessage}")
        case None => ;
      }
    }
  }

  private val props = new Properties()

  props.put("bootstrap.servers", "localhost:9092")
  props.put("acks", "all")
  props.put("enable.idempotence", "true")
  props.put("batch.size", "16384")
  props.put("linger.ms", "1")
  props.put("buffer.memory", "33554432")
  props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    private val producer = new KafkaProducer[Long, T](props)

    def send(topic: String, message: Tuple2[Long, T]): Unit = send(topic, Map(message))

    def send(topic: String, messages: Map[Long, T]): Unit = {

      logger.debug(s"sending batch with ${messages.size} messages to kafka queue")
      val messageResults = messages foreach { message =>
        producer.send(
          new ProducerRecord[Long, T](topic, message._1, message._2),
          futureCallback)
      }
    }
}