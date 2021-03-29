package com.restful

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.spark.ml.linalg.Vector


object rank_actor {
  def props(id: Int): Props = Props(new rank_actor(id))
  case class rankActorFeatures(features:Vector)
}

class rank_actor(id:Int) extends Actor with ActorLogging {
  import rank_actor._

  override def preStart(): Unit = log.info("rank actor id-{} started", id)

  override def postStop(): Unit = log.info("rank actor id-{} stopped", id)

  override def receive: Receive = {
    case f:rankActorFeatures=>
      sender() ! model.model_util.predictPerformance(f.features)
    case _ => None
  }
}

