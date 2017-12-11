package kafka

import java.util.Properties

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.{Logger, LoggerFactory}

class KafkaLoader {
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
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    private val producer = new KafkaProducer[String,String](props)

    def send(topic: String, message: String): Unit = send(topic, List(message))

    def send(topic: String, messages: Seq[String]): Unit = {

      logger.info("sending batch messages to kafka queue.......")
      val messageResults = messages.map {
        message => producer.send(
          new ProducerRecord[String, String](topic, message),
          futureCallback)
      }
    }
}