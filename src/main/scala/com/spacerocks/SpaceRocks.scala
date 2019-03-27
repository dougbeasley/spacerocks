package com.spacerocks

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.spacerocks.RockControlActor.GetControlFlow
import com.spacerocks.RockListenerActor.GetListenerFlow

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object SpaceRocks extends App {

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
  //TODO Move http stuff to a different file
  def actorFlow(ref : ActorRef): Flow[Message, Message, Any] = Flow[Message].ask[Message](ref)

  val route =
    path("control") {

      val handler = system.actorOf(RockControlActor.props(0, SpaceRockTopic))
      val futureFlow = (handler ? GetControlFlow) (3.seconds).mapTo[Flow[Message, Message, _]]

      onComplete(futureFlow) {
        case Success(flow) => handleWebSocketMessages(flow)
        case Failure(err) => complete(err.toString)
      }
    } ~
    path("listen") {

      val handler = system.actorOf(RockListenerActor.props(SpaceRockTopic))
      val futureFlow = (handler ? GetListenerFlow) (3.seconds).mapTo[Flow[Message, Message, _]]

      onComplete(futureFlow) {
        case Success(flow) => handleWebSocketMessages(flow)
        case Failure(err) => complete(err.toString)
      }
    } ~
    path("health") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, "{ status : \"OK\"}"))
      }
    }

  val bindingFuture = Http().bindAndHandle(cors() {route}, "0.0.0.0", 8080)

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up!")
  })

}
