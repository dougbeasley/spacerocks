package com.spacerocks

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.spacerocks.RockControlActor.GetControlFlow
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import com.spacerocks.RockListenerActor.GetListenerFlow

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object SpaceRocks extends App {

  implicit val askTimeout = Timeout(5.seconds)

  val config = ConfigFactory.load()
  val clusterName = config.getString("clustering.cluster.name")
  val SpaceRockTopic = "space-rocks"

  implicit val system = ActorSystem(clusterName)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")
  val subscriber = system.actorOf(RockListenerActor.props(SpaceRockTopic))

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

  /**
      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    */

}
