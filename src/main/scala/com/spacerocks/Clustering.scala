package com.spacerocks

import akka.actor.Props
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.spacerocks.SpaceRocks.system

trait Clustering {

  import system.log

  val cluster = Cluster(system)

  log.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up!")
    system.actorOf(Props[ClusterListener], name = "clusterListener")
  })
}
