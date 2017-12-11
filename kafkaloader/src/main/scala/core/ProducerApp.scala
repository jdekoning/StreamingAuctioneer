package core

import kafka.KafkaLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}
import java.util.concurrent.atomic.AtomicLong

object ProducerApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val topic = "auction-topic"
  val producer = new KafkaLoader[String]()
  val batchSize = 100
  val lastAuctionUpdate: AtomicLong = new AtomicLong(0)
  val auctionFetcher = new AuctionFetcher(lastAuctionUpdate)

  val t = new java.util.Timer()
  val scheduledLoader = new java.util.TimerTask {
    def run(): Unit = loadAuctionData()
  }
  t.schedule(scheduledLoader, 5000L, 5000L)

  def loadAuctionData(): Unit = {
    val futureAuctions = auctionFetcher.getNewAuctionData
    futureAuctions.onComplete {
      case Success(auctions) => logger.info(s"@${lastAuctionUpdate.get()} - successfully obtained ${auctions.length} auctions")
      case Failure(e) => logger.error(s"failed to get new auctionData: ${e.getClass} - ${e.getLocalizedMessage}")
    }

    futureAuctions.map(auctions => {
      val groupedAuctions = auctions.groupBy(_.item)
      logger.info(s"Sending ${auctions.length} auctions for ${groupedAuctions.size} items")
      groupedAuctions.grouped(batchSize).foreach { message =>
        logger.debug("Sending message batch size " + message.size)
        producer.send(topic, message.map(m => (m._1, m._2.toString)))
      }
    })
  }

}
