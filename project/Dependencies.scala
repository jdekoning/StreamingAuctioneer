import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val kafka = "org.apache.kafka" % "kafka-clients" % "1.0.0"
  lazy val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
  lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.25"
  lazy val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  lazy val play = "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3"
  lazy val playJson = "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3"
}
