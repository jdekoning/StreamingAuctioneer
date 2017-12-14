package core

import java.util.concurrent.atomic.AtomicLong

import client.{AuctionDataClient, PlayClient}
import core.identity.{Auction, AuctionData, Realm}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuctionFetcher(lastAuctionUpdate: AtomicLong, playClient: PlayClient)(implicit ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val auctionClient = new AuctionDataClient(playClient)
  val emptyAuctionData = Future(AuctionData(realms = Seq.empty[Realm], auctions = Seq.empty[Auction]))

  // Method will first get the auction status, if it is newer than the last it will execute the getAuctionData call.
  def getNewAuctionData(auctionFetchUrl: String): Future[Seq[Auction]] = {
    val statusFut = auctionClient.getAuctionStatus(auctionFetchUrl)
    val auctionData = statusFut.flatMap(status => {
      // Normally there should be only one file, without evidence of another, just get the head
      status.files.headOption match {
        case Some(file) =>
          // Here is uses the atomic Long to verify whether the auction data has been updated
          val lastModifiedTimestamp = file.lastModified
          if (lastModifiedTimestamp > lastAuctionUpdate.get()) {
            logger.info(s"new data found at timestamp: $lastModifiedTimestamp, doing getAuctionData call...")
            lastAuctionUpdate.set(lastModifiedTimestamp)
            auctionClient.getAuctionData(file)
          }
          else emptyAuctionData
        case None => emptyAuctionData
      }
    })

    auctionData.onComplete {
      case Success(aucData) => logger.debug(s"auctionData correctly fetched, length of file: ${aucData.auctions.length}")
      case Failure(e) => logger.error(s"failed api call: ${e.getClass} - ${e.getLocalizedMessage}")
    }

    auctionData.map(auctionData => auctionData.auctions)
  }

}
