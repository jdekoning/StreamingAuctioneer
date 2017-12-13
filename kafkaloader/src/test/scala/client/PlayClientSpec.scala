package client

import core.identity._
import core.json.AuctionJsonParser
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global

class PlayClientSpec extends FlatSpec with Matchers {
  val playClient = new PlayClient
}
