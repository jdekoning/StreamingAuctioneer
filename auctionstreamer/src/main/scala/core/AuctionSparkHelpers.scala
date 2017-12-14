package core

import core.identity.Auction
import core.json.AuctionJsonParser
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.dstream.DStream
import org.slf4j.{Logger, LoggerFactory}

object AuctionSparkHelpers {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  /** This method actually uses json to parse.
    * The data is actually stored as JSON on Kafka, an easy solution because the JSON parsers
    * were already available and tested
    */
  def mapStreamToAuctions(keyedRecords: DStream[(Long, String)]): DStream[(Long, List[Auction])] =
    keyedRecords.flatMapValues(AuctionJsonParser.bodyToAuctions)

  /** What defines the price is up to discussion, either the buyout or the current bid.
    * Usually buyouts happen a lot in WoW.
    */
  def mapAuctionsToAverage(keyedAuctions: DStream[(Long, List[Auction])]): DStream[(Long, Long)] =
    keyedAuctions.mapValues(values => values.map(_.buyout).sum / values.length)

  def getSumOfWindowByKey(keyedAveragePrice: DStream[(Long, Long)], windowDuration: Duration, slideDuration: Duration): DStream[(Long, Long)] =
    keyedAveragePrice.reduceByKeyAndWindow(sumReducer, windowDuration, slideDuration)

  def getSizeOfWindowByKey(keyedAveragePrice: DStream[(Long, Long)], windowDuration: Duration, slideDuration: Duration): DStream[(Long, Long)] =
    keyedAveragePrice.mapValues(_ => 1L).reduceByKeyAndWindow(sumReducer, windowDuration, slideDuration)

  //Meh all i want is the last value.. Probably much better way to do that ;)
  def getWindowAverage(keyedAveragePrice: DStream[(Long, Long)], windowDuration: Duration, slideDuration: Duration): DStream[(Long, Long)] =
    keyedAveragePrice.reduceByKeyAndWindow(takeRight, windowDuration, slideDuration)

  val sumReducer: (Long,Long) => Long = (a, b) => a + b
  val takeRight: (Long,Long) => Long = (a, b) => b
  def getAverage(sum: Long, count: Long): Long = (sum.toDouble/count.toDouble).toLong
  def percentIncrease(changeFromAverage: Double): Int = (changeFromAverage.abs*100).toInt
}
