package client

import core.identity._
import core.json.AuctionJsonParser
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}
import org.scalatest.easymock.EasyMockSugar
import org.easymock.EasyMock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Failure

class AuctionDataClientSpec extends FlatSpec with Matchers with EasyMockSugar with ScalaFutures {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit override val patienceConfig = PatienceConfig(
    timeout = Span(10, org.scalatest.time.Seconds),
    interval = Span(20, org.scalatest.time.Millis)
  )

  val auctionStatusString = "{\"files\":[{\"url\":\"http://auction-api-us.worldofwarcraft.com/auction-data/ab1239c3bc437d48321a64e6b5e5ab7f/auctions.json\",\"lastModified\":1513177124000}]}"
  val auctionStatusClass = AuctionStatus(Seq(File(url = "http://auction-api-us.worldofwarcraft.com/auction-data/ab1239c3bc437d48321a64e6b5e5ab7f/auctions.json", lastModified = 1513177124000L)))
  val auctionString = "[{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]"
  val auctionsClass = Seq(Auction(auc = 1535794057L, item = 37700L, owner = "Sornia", ownerRealm = "Exodar", bid = 219997L, buyout = 219997L, quantity = 1L, timeLeft = "LONG", rand = 0L, seed = 0L, context = 0L))
  val auctionDataString = "{\n\"realms\": [\n\t{\"name\":\"Medivh\",\"slug\":\"medivh\"},\n\t{\"name\":\"Exodar\",\"slug\":\"exodar\"}],\n\"auctions\": [{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]\n}"
  val auctionDataClass = AuctionData(realms = Seq(Realm(name = "Medivh", slug = "medivh"), Realm(name = "Exodar", slug = "exodar")), auctions = auctionsClass)

  val mockedClient = mock[PlayClient]

  expecting {
    expect(mockedClient.getCallStatus("some_url").andStubReturn(Future(Option(auctionStatusClass))))
    expect(mockedClient.getCallStatus("bad_url").andStubReturn(Future.failed[Option[AuctionStatus]](new Exception)))
    expect(mockedClient.getCallStatus("bad_json").andStubReturn(Future(None)))
    expect(mockedClient.getAuctionJson(anyObject[File]).andStubReturn(Future(Option(auctionDataClass))))
  }
  whenExecuting(mockedClient) {
    val auctionDataClient = new AuctionDataClient(mockedClient)

    it should "getAuctionStatus should return correctly return the class in happy flow" in {
      whenReady(auctionDataClient.getAuctionStatus("some_url")) { result =>
        result should be(auctionStatusClass)
      }
    }

    //TODO this is not going nice, needs fix
    ignore should "getAuctionStatus should return a failure if the service fails in any way" in {
      val result1 = Await.result(auctionDataClient.getAuctionStatus("bad_url"), 1 second)
      val result2 = Await.result(auctionDataClient.getAuctionStatus("bad_json"), 1 second)
      result1 shouldBe a [Failure[Option[AuctionStatus]]]
      result2 shouldBe a [Failure[Option[AuctionStatus]]]
    }

    it should "getAuctionData should return correctly return the class in happy flow" in {
      whenReady(auctionDataClient.getAuctionData(auctionStatusClass.files.head)) { result =>
        result should be(auctionDataClass)
      }
    }

    //TODO same tests as for getAuctionStatus, but needs fixing anyway
  }

}
