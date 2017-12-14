import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.6.0"
  lazy val easymock = "org.easymock" % "easymock" % "3.4"
  lazy val kafka = "org.apache.kafka" % "kafka-clients" % "1.0.0"
  lazy val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
  lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.25"
  lazy val play = "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3"
  lazy val playJson = "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3"
  lazy val spark = "org.apache.spark" %% "spark-core" % "2.2.1"
  lazy val sparkStreaming = "org.apache.spark" %% "spark-streaming" % "2.2.1"
  lazy val sparkKafka = "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.2.1"
  // Jackson scala had conflicts with spark & play, therefore i force this version. Align with other depdencies!
  lazy val jacksonScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.8"
  lazy val elasticSpark = "org.elasticsearch" % "elasticsearch-hadoop" % "6.0.1"
}
