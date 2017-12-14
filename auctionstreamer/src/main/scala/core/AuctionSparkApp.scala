package core

import core.identity.{AuctionWindow, VolatileWarning}
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.slf4j.{Logger, LoggerFactory}
import AuctionSparkHelpers._
import com.typesafe.config.ConfigFactory
import org.elasticsearch.spark.streaming._

object AuctionSparkApp extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load("auction-spark")

  val batchDuration = Milliseconds(config.getDuration("batch-duration").toMillis) //Meh no nice mapping from duration -> duration
  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("auction-spark-streaming").set("es.index.auto.create", "true")
  val streamingContext = new StreamingContext(conf, batchDuration)

  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> config.getString("kafka-hosts"),
    "key.deserializer" -> classOf[LongDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "use_a_separate_group_id_for_each_stream",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  //Should be a week window
  val windowDuration = Milliseconds(config.getDuration("window-duration").toMillis)
  val slideDuration = Milliseconds(config.getDuration("slide-duration").toMillis)
  val notificationWarningDiff = config.getDouble("warning-difference")

  val topics = Array("auction-topic")
  val stream: InputDStream[ConsumerRecord[Long, String]] = KafkaUtils.createDirectStream[Long, String](
    streamingContext,
    PreferConsistent,
    Subscribe[Long, String](topics, kafkaParams)
  )

  val recordTuple = stream.map(record => (record.key, record.value))
  val keyedAuctions = mapStreamToAuctions(recordTuple)
  val keyedAveragePrice = mapAuctionsToAverage(keyedAuctions).cache()
  val windowSumByKey = getSumOfWindowByKey(keyedAveragePrice, windowDuration, slideDuration)
  val windowSizeByKey = getSizeOfWindowByKey(keyedAveragePrice, windowDuration, slideDuration)
  val windowAverage = getWindowAverage(keyedAveragePrice, windowDuration, slideDuration)

  val joinedWindow = windowSumByKey.join(windowSizeByKey).join(windowAverage)
  val auctionWindowRDD = joinedWindow.map {case (item: Long, ((sum: Long, size: Long), lastAvg: Long)) => AuctionWindow(item, getAverage(sum, size), lastAvg)}
  auctionWindowRDD.saveToEs("auction/notification") //send basic info to elasticSearch
  val windowWithAverage = auctionWindowRDD.map(aWindow => Tuple2(aWindow, (aWindow.lastAvg.toDouble / aWindow.accumAvg) - 1D)).cache()
  val fastDropRDD = windowWithAverage.filter(cfa => cfa._2 < 0 && cfa._2.abs > notificationWarningDiff)
  fastDropRDD.map {case (aWindow:AuctionWindow, cfa: Double) =>
    VolatileWarning(aWindow.item, percentIncrease(cfa),
      slope = "negative", aWindow.accumAvg, aWindow.lastAvg)}.saveToEs("auction/notification")
  val fastRiseRDD = windowWithAverage.filter(cfa => cfa._2 > 0 && cfa._2.abs > notificationWarningDiff)
  fastRiseRDD.map { case (aWindow:AuctionWindow, cfa: Double) =>
      VolatileWarning(aWindow.item, percentIncrease(cfa),
      slope = "positive", aWindow.accumAvg, aWindow.lastAvg)}.saveToEs("auction/notification")


//  auctionWindowRDD.foreachRDD { rdd =>
//    rdd.foreach { record =>
//      val aWindow = record
//      logger.info(s"Found some amazing entries: item ${aWindow.item}, accumAvg ${aWindow.accumAvg}, lastAverage ${aWindow.lastAvg}")
//      val warningDifference = notificationWarningDiff
//      // Change will be between -1 and 1
//      val changeFromAverage = (aWindow.lastAvg.toDouble / aWindow.accumAvg) - 1D
//      if (changeFromAverage < 0 && changeFromAverage.abs > warningDifference) {
//        // If it went down by more than the set value send message with the percentage change
//        val pIncrease = percentIncrease(changeFromAverage)
//        logger.warn(s"The price of ${aWindow.item} went down by $pIncrease% from ${aWindow.accumAvg} to ${aWindow.lastAvg}")
//      }
//      if (changeFromAverage > 0 && changeFromAverage.abs > warningDifference) {
//        // If it went up by more than the set value send message with the percentage change
//        val pIncrease = percentIncrease(changeFromAverage)
//        logger.warn(s"The price of ${aWindow.item} went up by $pIncrease% from ${aWindow.accumAvg} to ${aWindow.lastAvg}")
//      }
//    }
//  }

  streamingContext.start()
  streamingContext.awaitTermination
}
