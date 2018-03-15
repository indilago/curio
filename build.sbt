name := "curio"

organization := "com.indilago"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.gilt" %% "gfc-aws-kinesis" % "0.16.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-dynamodb" % "0.16",
  "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % "0.16",
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.play" %% "play-json" % "2.6.8",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.mockito" % "mockito-core" % "2.15.0"
)