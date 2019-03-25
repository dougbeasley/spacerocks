package com.spacerocks

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.spacerocks.RockControlActor.{SpaceRock, UpdateResponse}
import spray.json.DefaultJsonProtocol

trait SpaceRockProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val rockFormat = jsonFormat7(SpaceRock.apply)
  implicit val responseFormat = jsonFormat1(UpdateResponse.apply)

}
