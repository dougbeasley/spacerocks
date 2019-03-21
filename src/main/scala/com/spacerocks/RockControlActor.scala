package com.spacerocks

import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.spacerocks.RockControlActor.{SpaceRock, UpdateResponse}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object RockControlActor {
  case class SpaceRock(name : String, color: String, size : Int, speed : Int, deltaX : Int, deltaY : Int)
  case class UpdateResponse(message : String)
  def props(id: Int): Props = Props(new RockControlActor(id))
}


class RockControlActor(id : Int) extends Actor with ActorLogging with SpaceRockProtocol {

  import akka.pattern.pipe
  import spray.json._

  implicit val as = context.system
  implicit val am = ActorMaterializer()

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: FiniteDuration = 5.seconds

  context.setReceiveTimeout(2 minutes)

  private var rock : Option[SpaceRock] = None

  def receive = {

    case tm: TextMessage =>
      tm.toStrict(timeout).map(_.text)
        .andThen {
          case Success(value) => log.info(value)
        }
        .flatMap(Unmarshal(_).to[SpaceRock])
        .andThen { case Success(r) => this.rock = Some(r) }
        .map(rock => UpdateResponse(s"Thanks for updating ${rock.name}"))
        .map(_.toJson.toString())
        .map(TextMessage.Strict(_)) pipeTo sender()

    case bm: BinaryMessage =>
      log.warning("Seeing binary data...")
      bm.dataStream.runWith(Sink.ignore)
      Future.failed(new Exception("yuck"))

    case ReceiveTimeout => context stop self //TODO Add message back to the client that the socket shutdown

  }

  override def preStart(): Unit = log.info("Staring RockControlActor")

  override def postStop(): Unit = log.info("Stopping RockControlActor")
}
