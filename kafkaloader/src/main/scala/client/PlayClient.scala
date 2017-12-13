package client

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws.ahc._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import core.identity.{AuctionData, AuctionStatus, File}
import core.json.AuctionJsonParser

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(validDataUrl: String)(implicit val ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val materializer = ActorMaterializer()
  val wsClient = StandaloneAhcWSClient()

  def getCallStatus(url: String): Future[Option[AuctionStatus]] = {
    wsClient.url(url).get().map { response =>
      val statusText: String = response.statusText
      val jsonBody = response.body
      logger.info(s"Got a response from getAuctionStatus $statusText")
      AuctionJsonParser.bodyToAuctionStatus(jsonBody)
    }
  }

  def getAuctionJson(file: File): Future[Option[AuctionData]] = {
    if (validateUrl(file.url)) {
      wsClient.url(file.url).get().map { response =>
        val statusText: String = response.statusText
        val jsonBody = response.body
        logger.info(s"Got a response from getAuctionData $statusText")
        AuctionJsonParser.bodyToAuctionData(jsonBody)
      }
    }
    else {
      logger.warn("url was not validated properly")
      Future(None)
    }
  }

  def validateUrl(url: String): Boolean = {
    val validUrl = url.contains(validDataUrl)
      if (!validUrl) {logger.warn(s"Returned $url does not contain $validDataUrl, will not execute datafetch call")}
    validUrl
  }

}
