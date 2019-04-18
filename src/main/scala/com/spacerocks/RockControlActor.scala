package com.spacerocks

import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object RockControlActor {
  case object GetControlFlow
  case class SpaceRock(id : String, name : String, color: String, size : Float, speed : Int, deltaX : Int, deltaY : Int)
  case class UpdateResponse(message : String)
  def props(id: Int, topic : String): Props = Props(new RockControlActor(id, topic))
}


class RockControlActor(id : Int, topic : String) extends Actor with ActorLogging with SpaceRockProtocol {

  import GraphDSL.Implicits._
  import RockControlActor._
  import spray.json._

  val mediator = DistributedPubSub(context.system).mediator


  implicit val as = context.system
  implicit val am = ActorMaterializer()

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: FiniteDuration = 5.seconds

  context.setReceiveTimeout(10 seconds)

  private var rock : Option[SpaceRock] = None


  val publisher = Source
    .actorRef[String](1000, OverflowStrategy.fail)
    .toMat(Sink.asPublisher(fanout = false))(Keep.right)
    .run()


  def receive = {

    case GetControlFlow =>

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

        val rockUpdateFlow = b.add(Flow[String]
             .mapAsync(1)(Unmarshal(_).to[SpaceRock]))

        val responseFlow = b.add(Flow[SpaceRock]
          .log(name = "rockStream")
          .map(rock => UpdateResponse(s"Thanks for updating ${rock.name}"))
          .map(_.toJson.toString())
        )

        //TODO may need to look into a better OnComplete response
        val pubSink = Flow[SpaceRock].map(Publish(topic, _)).to(Sink.actorRef(mediator, onCompleteMessage = "done"))
        val publish = b.add(pubSink)

        val pubSrc = b.add(Source.fromPublisher(publisher).map(TextMessage(_)))

        val bcast = b.add(Broadcast[SpaceRock](2))

        textMsgFlow ~> rockUpdateFlow ~> bcast ~> responseFlow ~> Sink.foreach[String](self ! _)
                                         bcast ~> publish

        FlowShape(textMsgFlow.in, pubSrc.out)
      })

      sender ! flow

    case ReceiveTimeout => context stop self //TODO Add message back to the client that the socket shutdown

  }

  override def preStart(): Unit = log.info("Staring RockControlActor")

  override def postStop(): Unit = log.info("Stopping RockControlActor")
}
