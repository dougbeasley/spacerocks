package com.spacerocks

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.spacerocks.RockControlActor.GetControlFlow
import com.typesafe.config.{Config, ConfigFactory}
import akka.pattern.ask
import com.spacerocks.RockListenerActor.GetListenerFlow

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object Rock1 extends App {
  new SpaceRocks(1)
}

object Rock2 extends App {
  new SpaceRocks(2)
}

object Rock3 extends App {
  new SpaceRocks(3)
}

class SpaceRocks(nr : Int) {

  implicit val askTimeout = Timeout(5.seconds)

//  val config = ConfigFactory.load()
//  val clusterName = config.getString("clustering.cluster.name")
  val SpaceRockTopic = "space-rocks"

  val config: Config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.hostname = "127.0.0.$nr"
      akka.management.http.hostname = "127.0.0.$nr"
    """).withFallback(ConfigFactory.load())
  val system = ActorSystem("space-rocks", config)

  AkkaManagement(system).start()

  ClusterBootstrap(system).start()

  Cluster(system).registerOnMemberUp({
    system.log.info("Cluster is up!")
  })

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")
  val subscriber = system.actorOf(RockListenerActor.props(SpaceRockTopic))


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

}
