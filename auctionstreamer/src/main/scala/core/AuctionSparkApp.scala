package core

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.slf4j.{Logger, LoggerFactory}

object AuctionSparkApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
  val streamingContext = new StreamingContext(conf, Seconds(1))

  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "use_a_separate_group_id_for_each_stream",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val topics = Array("auction-topic")
  val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
    streamingContext,
    PreferConsistent,
    Subscribe[String, String](topics, kafkaParams)
  )
  val recordTuple = stream.map(record => (record.key, record.value))
  recordTuple.print()
  streamingContext.start()
  streamingContext.awaitTermination
  logger.info("SOMETHING amazing")


}
