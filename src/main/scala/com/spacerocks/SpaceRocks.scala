//#full-example
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
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.io.StdIn

object SpaceRocks extends App {

  implicit val askTimeout = Timeout(5.seconds)

  val config = ConfigFactory.load()
  val clusterName = config.getString("clustering.cluster.name")

  implicit val system = ActorSystem(clusterName)
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")

  def handleWith(ref : ActorRef): Flow[Message, Message, Any] = Flow[Message].ask[Message](ref)


  val route =
    path("connect") {
      val handler = system.actorOf(RockControlActor.props(0))
      handleWebSocketMessages(handleWith(handler))
    } ~
    path("health") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, "{ status : \"OK\"}"))
      }
    }

  val bindingFuture = Http().bindAndHandle(cors() {route}, "0.0.0.0", 8080)

//  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
//  StdIn.readLine() // let it run until user presses return
//  bindingFuture
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ => system.terminate()) // and shutdown when done

}
//
//def main(args: Array[String]): Unit = {
//  if (args.isEmpty)
//  startup(Seq("2551", "2552", "0"))
//  else
//  startup(args)
//}
//
//  def startup(ports: Seq[String]): Unit = {
//  ports foreach { port =>
//  // Override the configuration of the port
//  // To use artery instead of netty, change to "akka.remote.artery.canonical.port"
//  // See https://doc.akka.io/docs/akka/current/remoting-artery.html for details
//  val config = ConfigFactory.parseString(s"""
//        akka.remote.netty.tcp.port=$port
//        """).withFallback(ConfigFactory.load())
//
//  // Create an Akka system
//  val system = ActorSystem("ClusterSystem", config)
//  // Create an actor that handles cluster domain events
//  system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
//}
//}