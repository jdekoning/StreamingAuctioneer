package kafka

import scala.collection.JavaConversions._
import core.identity.Auction
import core.json.AuctionJsonParser
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

  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "acks"-> "all",
    "enable.idempotence" -> "true",
    "batch.size" -> "16384",
    "linger.ms" -> "1",
    "buffer.memory" -> "33554432",
    "key.serializer" -> "org.apache.kafka.common.serialization.LongSerializer",
    "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
  )

  private val producer = new KafkaProducer[Long, String](kafkaParams)

  def send(topic: String, messages: Map[Long, Seq[Auction]]): Unit = {

    logger.debug(s"sending batch with ${messages.size} messages to kafka queue")
    messages foreach { message =>
      producer.send(
        new ProducerRecord[Long, String](topic, message._1, AuctionJsonParser.auctionsToStrings(message._2)),
        futureCallback)
    }
  }

}