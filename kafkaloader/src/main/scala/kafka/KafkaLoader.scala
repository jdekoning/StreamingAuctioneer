package kafka

import scala.collection.JavaConversions._
import core.identity.Auction
import core.json.AuctionJsonParser
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.{Logger, LoggerFactory}

class KafkaLoader(kafkaParams: Map[String, Object]) {

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