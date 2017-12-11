package client

import core.identity.{AuctionData, AuctionStatus, File}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuctionDataClient(implicit ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val playClient = new PlayClient()

  def getAuctionStatus(): Future[AuctionStatus] = {
    val getStatus = playClient.getCallStatus
    getStatus.onComplete {
      case Success(response) => logger.debug(s"response is: $response")
      case Failure(e) => logger.error(s"failed api call: ${e.getClass} - ${e.getLocalizedMessage}")
    }
    getStatus.flatMap {
      case Some(auctionStatus) =>
        logger.debug(s"response is: $auctionStatus")
        Future(auctionStatus)
      case None => logger.error(s"failed parsing of auctionStatus json object")
        Future.failed[AuctionStatus](new ClassCastException("failed parsing of auctionStatus json object"))
    }
  }

  def getAuctionData(file: File): Future[AuctionData] = {
    val getAuctionData = playClient.getAuctionJson(file)
    getAuctionData.onComplete {
      case Success(response) => logger.debug(s"response is: $response")
      case Failure(e) => logger.error(s"failed api call: ${e.getClass} - ${e.getLocalizedMessage}")
    }
    getAuctionData.flatMap {
      case Some(auctionData) =>
        logger.debug(s"response is: $auctionData")
        Future(auctionData)
      case None => logger.error(s"failed parsing of auctionData json object")
        Future.failed[AuctionData](new ClassCastException("failed parsing of auctionData json object"))
    }
  }
}
