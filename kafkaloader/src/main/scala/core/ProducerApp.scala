package core

import kafka.KafkaLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}
import java.util.concurrent.atomic.AtomicLong

import client.PlayClient
import com.typesafe.config.ConfigFactory

object ProducerApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val config = ConfigFactory.load("auction-kafka")

  // Creation of the kafka loader
  val topic = config.getString("kafka-topic")
  val batchSize = 100
  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> config.getString("kafka-hosts"),
    "acks"-> "all",
    "enable.idempotence" -> "true",
    "batch.size" -> "16384",
    "linger.ms" -> "1",
    "buffer.memory" -> "33554432",
    "key.serializer" -> "org.apache.kafka.common.serialization.LongSerializer",
    "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
  )
  val kafkaLoader = new KafkaLoader(kafkaParams)

  // The client that will call the WoW auction API
  val apiKey = config.getString("api-key")
  val validDataUrl = config.getString("valid-data-url")
  val auctionFetcherUrl = s"${config.getString("auction-fetcher-url")}$apiKey"
  val playClient = new PlayClient(validDataUrl)

  /**
    * AtomicLong to avoid immediate concurrancy problems
    * If load really increases maybe use an external system to maintain such a counter
    */
  val lastAuctionUpdate: AtomicLong = new AtomicLong(0)
  val auctionFetcher = new AuctionFetcher(lastAuctionUpdate, playClient)

  // Schedule the loader to poll and if there is new data, get and push it to Kafka
  val t = new java.util.Timer()
  val scheduledLoader = new java.util.TimerTask {
    def run(): Unit = loadAuctionData()
  }
  t.schedule(scheduledLoader, 5000L, config.getLong("schedule-await"))

  // Method that first does the api calls and then uses the kafkaLoader to push it to Kafka
  // TODO Below method can be moved elsewhere, does not really belong in the App/Main
  def loadAuctionData(): Unit = {
    val futureAuctions = auctionFetcher.getNewAuctionData(auctionFetcherUrl)
    futureAuctions.onComplete {
      case Success(auctions) => logger.info(s"@${lastAuctionUpdate.get()} - successfully obtained ${auctions.length} auctions")
      case Failure(e) => logger.error(s"failed to get new auctionData: ${e.getClass} - ${e.getLocalizedMessage}")
    }

    futureAuctions.map(auctions => {
      val groupedAuctions = auctions.groupBy(_.item)
      logger.info(s"Sending ${auctions.length} auctions for ${groupedAuctions.size} items")
      groupedAuctions.grouped(batchSize).foreach { message =>
        logger.debug("Sending message batch size " + message.size)
        kafkaLoader.send(topic, message.map(m => (m._1, m._2)))
      }
    })
  }
}
