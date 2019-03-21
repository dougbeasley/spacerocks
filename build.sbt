name := "space-rocks"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.21"
lazy val akkaHttpVersion = "10.1.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  "ch.megard" %% "akka-http-cors" % "0.4.0",

  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

version in Docker := "latest"

dockerExposedPorts in Docker := Seq(1600)

dockerEntrypoint in Docker := Seq("sh", "-c", "bin/clustering $*")

dockerRepository := Some("lightbend")

dockerBaseImage := "java"
enablePlugins(JavaAppPackaging)