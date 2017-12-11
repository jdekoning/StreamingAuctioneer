package core

import java.util.concurrent.atomic.AtomicLong

import client.AuctionDataClient
import core.identity.{Auction, AuctionData, Realm}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuctionFetcher(lastAuctionUpdate: AtomicLong)(implicit ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val auctionClient = new AuctionDataClient()
  val emptyAuctionData = Future(AuctionData(realms = Seq.empty[Realm], auctions = Seq.empty[Auction]))
  //
  //  lastAuctionUpdate.set(currentAuctionTimestamp + 1)

  def getNewAuctionData: Future[Seq[Auction]] = {
    val statusFut = auctionClient.getAuctionStatus()
    val auctionData = statusFut.flatMap(status => {
      // Normally there should be only one file, without evidence of another, just get the head
      status.files.headOption match {
        case Some(file) =>
          if (file.lastModified > lastAuctionUpdate.get()) {
            lastAuctionUpdate.set(file.lastModified)
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
