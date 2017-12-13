package json

import core.identity._
import core.json.AuctionJsonParser
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class AuctionJsonParserSpec extends FlatSpec with Matchers {
  val logger = LoggerFactory.getLogger(getClass)
  val faultyString = "{}"
  val auctionStatusString = "{\"files\":[{\"url\":\"http://auction-api-us.worldofwarcraft.com/auction-data/ab1239c3bc437d48321a64e6b5e5ab7f/auctions.json\",\"lastModified\":1513177124000}]}"
  val auctionStatusClass = AuctionStatus(Seq(File(url = "http://auction-api-us.worldofwarcraft.com/auction-data/ab1239c3bc437d48321a64e6b5e5ab7f/auctions.json", lastModified = 1513177124000L)))
  val auctionString = "[{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]"
  val auctionsClass = Seq(Auction(auc = 1535794057L, item = 37700L, owner = "Sornia", ownerRealm = "Exodar", bid = 219997L, buyout = 219997L, quantity = 1L, timeLeft = "LONG", rand = 0L, seed = 0L, context = 0L))
  val auctionDataString = "{\n\"realms\": [\n\t{\"name\":\"Medivh\",\"slug\":\"medivh\"},\n\t{\"name\":\"Exodar\",\"slug\":\"exodar\"}],\n\"auctions\": [{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]\n}"
  val auctionDataClass = AuctionData(realms = Seq(Realm(name = "Medivh", slug = "medivh"), Realm(name = "Exodar", slug = "exodar")), auctions = auctionsClass)

  it should "bodyToAuctionStatus should return None if given an empty json" in {
    AuctionJsonParser.bodyToAuctionStatus(faultyString).isEmpty should be(true)
  }

  it should "bodyToAuctionStatus should return a correct object when given a correct json" in {
    val auctionStatus = AuctionJsonParser.bodyToAuctionStatus(auctionStatusString)
    auctionStatus.isDefined should be(true)
    auctionStatus.get should be(auctionStatusClass)
  }

  it should "bodyToAuction should return None if given an empty json" in {
    AuctionJsonParser.bodyToAuctions(faultyString).isEmpty should be(true)
  }

  it should "bodyToAuction should return a correct object when given a correct json" in {
    val auction = AuctionJsonParser.bodyToAuctions(auctionString)
    auction.isDefined should be(true)
    auction.get should be(auctionsClass)
  }

  it should "bodyToAuctionData should return None if given an empty json" in {
    AuctionJsonParser.bodyToAuctionData(faultyString).isEmpty should be(true)
  }

  it should "bodyToAuctionData should return a correct object when given a correct json" in {
    val auction = AuctionJsonParser.bodyToAuctionData(auctionDataString)
    auction.isDefined should be(true)
    auction.get should be(auctionDataClass)
  }


}
