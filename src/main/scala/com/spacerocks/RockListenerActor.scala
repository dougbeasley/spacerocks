package com.spacerocks

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import com.spacerocks.RockControlActor.SpaceRock

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object RockListenerActor {
  case object GetListenerFlow
  def props(topic : String): Props = Props(new RockListenerActor(topic))
}


class RockListenerActor(topic : String) extends Actor with ActorLogging with SpaceRockProtocol {

  import GraphDSL.Implicits._
  import RockListenerActor._
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import spray.json._

  implicit val as = context.system
  implicit val am = ActorMaterializer()

  implicit val ec: ExecutionContext = context.dispatcher

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(topic, self)

  val (listener, publisher) = Source
    .actorRef[String](1000, OverflowStrategy.fail)
    .toMat(Sink.asPublisher(fanout = false))(Keep.both)
    .run()

  def receive = {

    case rock: SpaceRock =>
      log.info(rock.toString)
      listener ! rock.toJson.toString()

    case GetListenerFlow =>
      val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>

        // only works with TextMessage. Extract the body and sends it to self
        val textMsgFlow = b.add(Flow[Message]
          .mapAsync(1) {
            case tm: TextMessage => tm.toStrict(3.seconds).map(_.text)
            case bm: BinaryMessage =>
              log.warning("Seeing binary data...")
              bm.dataStream.runWith(Sink.ignore)
              Future.failed(new Exception("bork!"))
          })

        val pubSrc = b.add(Source.fromPublisher(publisher).map(TextMessage(_)))

        textMsgFlow ~> Sink.foreach[String](self ! _)

        FlowShape(textMsgFlow.in, pubSrc.out)
      })

      sender ! flow

    case SubscribeAck(Subscribe(`topic`, None, `self`)) ⇒
      log.info("subscribing")

  }
}


