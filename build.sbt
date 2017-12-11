import Dependencies._

lazy val commonSettings = Seq(
  organization := "nl.koning",
  scalaVersion := "2.11.11",
  version      := "0.1.0-SNAPSHOT"
)

lazy val kafkaloader = (project in file("kafkaloader")).
  settings(
    commonSettings,
    name:= "KafkaLoader",
    libraryDependencies ++= Seq(
      scalaTest % Test, slf4j, slf4jSimple, kafka, play, playJson
    )
  )

lazy val auctionstreamer = (project in file("auctionstreamer")).
  settings(
    commonSettings,
    name:= "AuctionStreamer",
    libraryDependencies ++= Seq(
      scalaTest % Test, slf4j, slf4jSimple, spark, sparkKafka, sparkStreaming
    )
  )