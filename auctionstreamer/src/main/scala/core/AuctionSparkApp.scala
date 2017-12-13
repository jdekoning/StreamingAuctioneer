package core

import core.identity.AuctionWindow
import core.json.AuctionJsonParser
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.slf4j.{Logger, LoggerFactory}

object AuctionSparkApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
  val streamingContext = new StreamingContext(conf, Seconds(30))

  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[LongDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "use_a_separate_group_id_for_each_stream",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  //Should be a week window
  val windowDuration = Minutes(60)
  val slideDuration = Minutes(1)

  val topics = Array("auction-topic")
  val stream: InputDStream[ConsumerRecord[Long, String]] = KafkaUtils.createDirectStream[Long, String](
    streamingContext,
    PreferConsistent,
    Subscribe[Long, String](topics, kafkaParams)
  )

  val recordTuple = stream.map(record => (record.key, record.value))
  val keyedAuctions = recordTuple.flatMapValues(AuctionJsonParser.bodyToAuctions)
  val keyedAveragePrice = keyedAuctions.mapValues(values => values.map(_.buyout).sum / values.length).cache()
  val SUM_REDUCER: (Long,Long) => Long = (a, b) => a + b
  val windowSumByKey = keyedAveragePrice.reduceByKeyAndWindow(SUM_REDUCER, windowDuration, slideDuration)
  val windowSizeByKey = keyedAveragePrice.mapValues(_ => 1L).reduceByKeyAndWindow(SUM_REDUCER, windowDuration, slideDuration)
  val windowAverage = keyedAveragePrice.window(windowDuration, slideDuration)

  val joinedWindow = windowSumByKey.join(windowSizeByKey).join(windowAverage)

  joinedWindow.foreachRDD { rdd =>
    rdd.foreach { record =>
      val aWindow = AuctionWindow(record._1, record._2._1._1 / record._2._1._2, record._2._2)
      logger.info(s"Found some amazing entries: item ${aWindow.item}, 1 hour Avg: ${aWindow.accumAvg}, lastAverage ${aWindow.lastAvg}")
      if (aWindow.lastAvg > aWindow.accumAvg) {
        val percentIncrease = ((aWindow.lastAvg.toDouble / aWindow.accumAvg.toDouble - 1)*100).floor
        logger.warn(s"The price of ${aWindow.item} went up by $percentIncrease%")
      }
    }
  }

  streamingContext.start()
  streamingContext.awaitTermination
}
