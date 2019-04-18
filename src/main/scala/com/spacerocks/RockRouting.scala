package com.spacerocks

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import com.spacerocks.RockControlActor.GetControlFlow
import com.spacerocks.RockListenerActor.GetListenerFlow
import com.spacerocks.SpaceRocks.{SpaceRockTopic, system}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

final case class HealthCheck(status: String = "OK")

trait RockRouting extends Directives with SpaceRockProtocol {

  val routes =
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
        complete(HealthCheck())
      }
    }


}
