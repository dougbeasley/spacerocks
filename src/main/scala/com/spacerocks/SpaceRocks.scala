package com.spacerocks

import akka.actor.{ActorSystem, Props}
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.concurrent.duration._


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

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")
  val subscriber = system.actorOf(RockListenerActor.props(SpaceRockTopic))

  implicit val askTimeout = Timeout(5.seconds)


  val healthRoutes = path("alive") {
      complete(StatusCodes.OK)
    } ~
    path("ready") {
      val selfState = cluster.selfMember.status
      log.debug(s"ready? clusterState: $selfState")

      selfState match {
        case MemberStatus.Up => complete(StatusCodes.OK)
        case MemberStatus.WeaklyUp => complete(StatusCodes.OK)
        case _ => complete(StatusCodes.InternalServerError)
      }
    }



  val bindingFuture = Http().bindAndHandle(cors() { routes } ~ healthRoutes, "0.0.0.0", 8080)

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up!")
  })

}
