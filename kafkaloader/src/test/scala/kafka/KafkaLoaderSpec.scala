package kafka

import core.identity._
import core.json.AuctionJsonParser
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class KafkaLoaderSpec extends FlatSpec with Matchers {
  val kafkaParams: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "acks"-> "all",
    "enable.idempotence" -> "true",
    "batch.size" -> "16384",
    "linger.ms" -> "1",
    "buffer.memory" -> "33554432",
    "key.serializer" -> "org.apache.kafka.common.serialization.LongSerializer",
    "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
  )
  it should "be possible to initialize the kafka loader" in {
    //Succeeds as long as there is no error
    val kafkaLoader = new KafkaLoader(kafkaParams)
  }

  //TODO add tests here. Needs some kafka integration
}
