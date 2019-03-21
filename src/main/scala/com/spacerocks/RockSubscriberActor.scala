package com.spacerocks

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import com.spacerocks.RockControlActor.SpaceRock


object RockSubscriberActor {
  def props(topic : String): Props = Props(new RockSubscriberActor(topic))
}


class RockSubscriberActor(topic : String) extends Actor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  val mediator = DistributedPubSub(context.system).mediator
  // subscribe to the topic named "content"
  mediator ! Subscribe(topic, self)

  def receive = {
    case rock: SpaceRock =>
      log.info(rock.toString)

    case s: String ⇒
      log.info("Got {}", s)

    case SubscribeAck(Subscribe("content", None, `self`)) ⇒
      log.info("subscribing")
  }
}


