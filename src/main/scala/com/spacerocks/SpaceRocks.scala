package com.spacerocks

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._


object SpaceRocks extends App with RockRouting {

  import system.log

  val SpaceRockTopic = "space-rocks"

  implicit val system = ActorSystem("space-rocks")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val cluster = Cluster(system)

  log.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  val bindingFuture = Http().bindAndHandle(cors() { routes }, "0.0.0.0", 8080)

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up!")
    system.actorOf(Props[ClusterListener], name = "clusterListener")
  })

}
