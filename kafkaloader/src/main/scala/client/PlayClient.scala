package client

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws.ahc._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import core.identity.{AuctionData, AuctionStatus, File}

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(implicit val ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  val auctionJsonParser = new AuctionJsonParser
  implicit val materializer = ActorMaterializer()
  val wsClient = StandaloneAhcWSClient()
  private val url = "https://us.api.battle.net/wow/auction/data/medivh?locale=en_US&apikey=5he762e8ugx2tydz3zjkpnjwrt9vqv5u"

  def getCallStatus: Future[Option[AuctionStatus]] = {
    wsClient.url(url).get().map { response =>
      val statusText: String = response.statusText
      val jsonBody = response.body
      logger.info(s"Got a response from getAuctionStatus $statusText")
      auctionJsonParser.bodyToAuctionStatus(jsonBody)
    }
  }

  def getAuctionJson(file: File): Future[Option[AuctionData]] = {
    if (validateUrl(file.url)) {
      wsClient.url(file.url).get().map { response =>
        val statusText: String = response.statusText
        val jsonBody = response.body
        logger.info(s"Got a response from getAuctionData $statusText")
        auctionJsonParser.bodyToAuctionData(jsonBody)
      }
    }
    else {
      logger.warn("url was not validated properly")
      Future(None)
    }
  }

  def validateUrl(url: String): Boolean = {
    url.contains("http://auction-api-us.worldofwarcraft.com/auction-data/")
  }


}
