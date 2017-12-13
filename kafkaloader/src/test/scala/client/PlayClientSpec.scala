package client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import core.identity._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global

class PlayClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val hostPort = "http://localhost:9191"
  val auctionUrl = "/wow/auction/data/"
  val auctionDataUrl = "/auction-data/"
  val playClient = new PlayClient(auctionDataUrl)

  implicit override val patienceConfig = PatienceConfig(
    timeout = Span(10, org.scalatest.time.Seconds),
    interval = Span(20, org.scalatest.time.Millis)
  )

  val auctionStatusString = "{\"files\":[{\"url\":\"http://localhost:9191/auction-data/\",\"lastModified\":1513177124000}]}"
  val auctionStatusClass = AuctionStatus(Seq(File(url = hostPort + auctionDataUrl, lastModified = 1513177124000L)))
  val auctionString = "[{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]"
  val auctionsClass = Seq(Auction(auc = 1535794057L, item = 37700L, owner = "Sornia", ownerRealm = "Exodar", bid = 219997L, buyout = 219997L, quantity = 1L, timeLeft = "LONG", rand = 0L, seed = 0L, context = 0L))
  val auctionDataString = "{\n\"realms\": [\n\t{\"name\":\"Medivh\",\"slug\":\"medivh\"},\n\t{\"name\":\"Exodar\",\"slug\":\"exodar\"}],\n\"auctions\": [{\"auc\":1535794057,\"item\":37700,\"owner\":\"Sornia\",\"ownerRealm\":\"Exodar\",\"bid\":219997,\"buyout\":219997,\"quantity\":1,\"timeLeft\":\"LONG\",\"rand\":0,\"seed\":0,\"context\":0}]\n}"
  val auctionDataClass = AuctionData(realms = Seq(Realm(name = "Medivh", slug = "medivh"), Realm(name = "Exodar", slug = "exodar")), auctions = auctionsClass)

  val wiremockServer = new WireMockServer((new WireMockConfiguration).port(9191))
  override def beforeAll(): Unit = {
    wiremockServer.start()
    wiremockServer.stubFor(get(urlEqualTo(auctionUrl))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type","application/json")
          .withFixedDelay(50)
          .withBody(auctionStatusString)
      ))
    wiremockServer.stubFor(get(urlEqualTo(auctionDataUrl))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type","application/json")
          .withFixedDelay(1000)
          .withBody(auctionDataString)
      ))
  }

  it should "correctly check whether the url is valid from the returned status call" in {
    val validUrl = hostPort + "/auction-data/data/more/extras"
    val inValidUrl = hostPort + "/missing/aution-data/at/beginning"
    playClient.validateUrl(validUrl) should be(true)
    playClient.validateUrl(inValidUrl) should be(false)
  }

  it should "return the right json when getCallStatus is called" in {
    whenReady(playClient.getCallStatus(hostPort + auctionUrl)) {result =>
      result.nonEmpty should be(true)
      result.get should be(auctionStatusClass)
    }
  }

  it should "return the right json when getAuctionJson is called" in {
    whenReady(playClient.getAuctionJson(auctionStatusClass.files.head)) {result =>
      result.nonEmpty should be(true)
      result.get should be(auctionDataClass)
    }
  }
  //Todo add some more failure cases here
}
