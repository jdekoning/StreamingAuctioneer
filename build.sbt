import Dependencies._

lazy val commonSettings = Seq(
  organization := "nl.koning",
  scalaVersion := "2.12.3",
  version      := "0.1.0-SNAPSHOT"
)

lazy val root = (project in file(".")).
  settings(
    commonSettings,
    name := "Hello",
    libraryDependencies ++= Seq(
      scalaTest % Test
    )
  )


lazy val kafkaloader = (project in file("kafkaloader")).
  settings(
    commonSettings,
    name:= "KafkaLoader",
    libraryDependencies ++= Seq(
      scalaTest % Test, slf4j, slf4jSimple, kafka, play, playJson
    )
  )