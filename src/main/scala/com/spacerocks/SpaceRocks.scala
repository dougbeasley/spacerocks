package com.spacerocks

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._


object SpaceRocks extends App with RockRouting with Clustering {

  implicit val system = ActorSystem("space-rocks")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bindingFuture = Http().bindAndHandle(cors() { routes }, "0.0.0.0", 8080)

}
