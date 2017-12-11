package core

import client.PlayClient
import kafka.KafkaLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProducerApp extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val topic = "demo-topic"
  val producer = new KafkaLoader()
  val playClient = new PlayClient()
  val batchSize = 100

  val status = doOneCall()
  val data = status.flatMap(doSecondCall)

  def doOneCall(): Future[Option[AuctionStatus]] = {
    val getStatus = playClient.getCallStatus
    getStatus.onComplete {
      case Success(response) => logger.info(s"response is: $response")
      case Failure(e) => logger.error(s"failed api call: ${e.getClass} - ${e.getLocalizedMessage}")
    }
    getStatus
  }

  def doSecondCall(first: Option[AuctionStatus]): Future[Seq[Option[AuctionData]]] = {
    val getData = first match {
      case Some(auctionStatus) =>
        val files: Seq[File] = auctionStatus.files
        Future.sequence(files.map(file => playClient.getAuctionJson(file)))

      case None =>
        logger.error(s"Api call for auctionStatus succeeded but parsing failed")
        Future(List.empty[Option[AuctionData]])
    }
    getData.onComplete {
      case Success(response) => logger.info(s"response is: $response")
      case Failure(e) => logger.error(s"failed api call: ${e.getClass} - ${e.getLocalizedMessage}")
    }
    getData
  }




//  (1 to 1000000).toList.map(no => "Message " + no).grouped(batchSize).foreach { message =>
//    logger.info("Sending message batch size " + message.length)
//    producer.send(topic, message)
//  }

}
