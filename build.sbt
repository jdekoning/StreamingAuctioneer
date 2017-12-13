import Dependencies._

lazy val commonSettings = Seq(
  organization := "nl.koning",
  scalaVersion := "2.11.11",
  version      := "0.1.0-SNAPSHOT"
)

lazy val auctionidentity = (project in file("auctionidentity"))
  .settings(
    commonSettings,
    name:= "AuctionIdentity",
    libraryDependencies ++= Seq(
      slf4j, slf4jSimple, playJson,
      scalaTest % Test
    )
  )

lazy val kafkaloader = (project in file("kafkaloader"))
  .dependsOn(auctionidentity)
  .settings(
    commonSettings,
    name:= "KafkaLoader",
    libraryDependencies ++= Seq(
      slf4j, slf4jSimple, kafka, play, playJson,
      scalaTest % Test
    )
  )

lazy val auctionstreamer = (project in file("auctionstreamer"))
  .dependsOn(auctionidentity)
  .settings(
    commonSettings,
    name:= "AuctionStreamer",
    libraryDependencies ++= Seq(
      slf4j, slf4jSimple, spark, playJson, sparkKafka, sparkStreaming, jacksonScala,
      scalaTest % Test
    )
  )