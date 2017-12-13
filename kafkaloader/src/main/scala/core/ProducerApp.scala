package core

import kafka.KafkaLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}
import java.util.concurrent.atomic.AtomicLong

import client.PlayClient

object ProducerApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val topic = "auction-topic"
  val apiKey = "mh2dyh3fh2jr2wet6uhd3jjkkecn6js8"
  val auctionFetcherUrl = s"https://us.api.battle.net/wow/auction/data/medivh?locale=en_US&apikey=$apiKey"
  val validDataUrl = "http://auction-api-us.worldofwarcraft.com/auction-data/"

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
  val batchSize = 100
  val kafkaLoader = new KafkaLoader(kafkaParams)
  val playClient = new PlayClient(validDataUrl)
  val lastAuctionUpdate: AtomicLong = new AtomicLong(0)
  val auctionFetcher = new AuctionFetcher(lastAuctionUpdate, playClient)

  val t = new java.util.Timer()
  val scheduledLoader = new java.util.TimerTask {
    def run(): Unit = loadAuctionData()
  }
  t.schedule(scheduledLoader, 5000L, 5000L)

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
