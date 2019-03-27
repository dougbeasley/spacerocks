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
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,

  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.0",
  "ch.megard" %% "akka-http-cors" % "0.4.0",

  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)


enablePlugins(JavaAppPackaging)

packageName in Docker := packageName.value

version in Docker := version.value

import com.typesafe.sbt.packager.docker._

daemonUserUid in Docker := None
daemonUser in Docker    := "daemon"

dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }


dockerExposedPorts := Seq(8080, 8558, 2552)
dockerBaseImage := "openjdk:8-jre-alpine"

dockerCommands ++= Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "bind-tools", "busybox-extras", "curl", "strace"),
  Cmd("RUN", "chgrp -R 0 . && chmod -R g=u .")
)