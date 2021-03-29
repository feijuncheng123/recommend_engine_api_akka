package com.restful.actor

import akka.actor.{Actor, ActorLogging, Props}


object log_actor {
  def props(): Props = Props(new log_actor())
  case class loggerInfo(template: String)
  case class loggerWarn(template: String)
  case class loggerError(template: String)
}

class log_actor extends Actor with ActorLogging {
  import log_actor._
  override def preStart(): Unit = log.info("logger actor started")

  override def postStop(): Unit = log.info("logger actor stopped")


  override def receive: Receive = {
    case logger:loggerInfo=> log.info(logger.template)
    case logger:loggerWarn=> log.warning(logger.template)
    case logger:loggerError=> log.error(logger.template)
    case _ => log.warning("未知消息！")
  }
}
