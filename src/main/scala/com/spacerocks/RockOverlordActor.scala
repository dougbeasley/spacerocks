package com.spacerocks

import akka.actor.{Actor, ActorLogging}
import akka.cluster.pubsub.DistributedPubSub

class RockOverlordActor extends Actor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  val mediator = DistributedPubSub(context.system).mediator
  // subscribe to the topic named "content"
  mediator ! Subscribe("content", self)

  def receive = {
    case s: String ⇒
      log.info("Got {}", s)
    case SubscribeAck(Subscribe("content", None, `self`)) ⇒
      log.info("subscribing")
  }
}


