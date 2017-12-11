package client

import core.identity._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

class AuctionJsonParser {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val fileReads = Json.reads[File]
  implicit val auctionStatusReads = Json.reads[AuctionStatus]
  implicit val realmReads = Json.reads[Realm]
  implicit val auctionReads = Json.reads[Auction]
  implicit val auctionDataReads = Json.reads[AuctionData]

  def bodyToAuctionStatus(body: String): Option[AuctionStatus] = {
    val json = Json.parse(body)
    val statusFromJson: JsResult[AuctionStatus] = Json.fromJson[AuctionStatus](json)
    statusFromJson match {
      case JsSuccess(as: AuctionStatus, path: JsPath) => Some(as)
      case e: JsError =>
        logger.info(s"Error encountered: ${e.getClass} - ${JsError.toJson(e).toString()}")
        None
    }
  }

  def bodyToAuctionData(body: String): Option[AuctionData] = {
    val json = Json.parse(body)
    val statusFromJson: JsResult[AuctionData] = Json.fromJson[AuctionData](json)
    statusFromJson match {
      case JsSuccess(as: AuctionData, path: JsPath) => Some(as)
      case e: JsError =>
        logger.info(s"Error encountered: ${e.getClass} - ${JsError.toJson(e).toString()}")
        None
    }
  }
}