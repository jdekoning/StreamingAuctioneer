package core

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

object AuctionSparkApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
  val streamingContext = new StreamingContext(conf, Seconds(1))

  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[LongDeserializer],
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

  case class Auction(auc: Long, item: Long, owner: String, ownerRealm: String, bid: Long, buyout: Long, quantity: Long, timeLeft: String, rand: Long, seed:Long, context:Long)
  implicit val auctionReads = Json.reads[Auction]
  def bodyToAuctionStatus(body: String): Option[List[Auction]] = {
    val json = Json.parse(body)
    val statusFromJson: JsResult[List[Auction]] = Json.fromJson[List[Auction]](json)
    statusFromJson match {
      case JsSuccess(as: List[Auction], path: JsPath) => Some(as)
      case e: JsError =>
        logger.info(s"Error encountered: ${e.getClass} - ${JsError.toJson(e).toString()}")
        None
    }
  }

  val recordTuple = stream.map(record => (record.key, record.value))
  val keyedAuctions = recordTuple.flatMapValues(bodyToAuctionStatus)
  keyedAuctions.print()

  streamingContext.start()
  streamingContext.awaitTermination
  logger.info("SOMETHING amazing")


}
