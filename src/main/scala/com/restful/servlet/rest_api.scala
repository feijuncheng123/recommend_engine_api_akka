package com.restful

import java.io.File
import java.util
import org.apache.spark.ml.linalg.Vectors
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.{Await, ExecutionContext}
//import org.scalatra.ScalatraServlet
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

//import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import _root_.akka.actor.{Actor, ActorRef, ActorSystem,Props}
import _root_.akka.pattern.{ ask, pipe }
import _root_.akka.util.Timeout


/**
 * rest api处理逻辑
 * @param system  ActorSystem系统，用于管理actor线程池
 */
class rest_api(system:ActorSystem) extends ScalatraServlet with JacksonJsonSupport with FutureSupport {

  //独立定义的actor，其他actor无法获取时使用这个
  private val standbyActor=system.actorOf(Props(new rank_actor(10086)))

  override protected implicit def executor: ExecutionContext = global
  implicit val timeout: Timeout = Timeout(5.seconds)
  protected implicit lazy val jsonFormats: Formats = DefaultFormats
  val logger: Logger = LoggerFactory.getLogger(getClass)

  //解析的特征字段
  private val modelCols=Array("col1","col2","col3","col4","col5", "col6","col7")

  //线程池
  val rankToActor: Map[Int, ActorRef] = (for(i <- 0 until 200) yield {
    val rankActor = system.actorOf(rank_actor.props(i), s"rankActor-$i")
    (i,rankActor)}).toMap

  //embedding特征为空时以0值填充
  private val zeroString=Array.fill(50)(0.0).mkString(",")

  before() {
    contentType = formats("json")
  }

  /**
   * 预测api，通过分发到actor上请求预测，返回map
   */
  post("/api"){
    val input= parsedBody.extract[data_input]
    val queryRedis=RedisConnection.get(input.usr_id)
    queryRedis match {
      case Some(goods)=>
        val probability=goods.asScala.zipWithIndex.map{case ((key:String,value_java:util.HashMap[String,String]),index:Int)=>
          val value=value_java.asScala
          val userFeatures_fillna = value.getOrElse("embedding_user_feature", zeroString).split(",").map(_.toDouble)
          val itemFeatures_fillna = value.getOrElse("embedding_item_feature", zeroString).split(",").map(_.toDouble)
          val features = modelCols.map(value.getOrElse(_, "0").toDouble) ++ userFeatures_fillna ++ itemFeatures_fillna
          val features_vector = Vectors.dense(features)
          val exc_actor=rankToActor.getOrElse(index,standbyActor)
          val msg=ask(exc_actor,rank_actor.rankActorFeatures(features_vector))
          Await.result(msg,1.second)
        }
        Map("usr_id"->input.usr_id,"goods_rank"->probability)
      case _ => Map("usr_id"->input.usr_id,"goods_rank"->"")
    }
  }

  /**
   * 模型更新api，传入本地路径后重新加载模型，返回map
   */
  post("/update_model"){
    logger.info(request.body)
    val input= parsedBody.extract[modelPath]
    val gbdt_path=new File(input.gbdt_path)
    val lr_path=new File(input.lr_path)
    if(gbdt_path.exists()){
      if(lr_path.exists())
      {logger.info("路径检测正确！开始更新模型！")
        model.model_util=model.load_model(input.gbdt_path,input.lr_path)
        model.current_model("gbdt_path")=input.gbdt_path
        model.current_model("lr_path")=input.lr_path
        Map("info"->"更新成功","status"->"0","debug"->"","gbdt_path"->input.gbdt_path,"lr_path"->input.lr_path)}
      else { logger.info("lr模型路径不存在！");  Map("info"->"更新失败！","status"->"1","debug"->s"lr模型路径不存在！请检查！${input.lr_path}","gbdt_path"->model.current_model("gbdt_path"),"lr_path"-> model.current_model("lr_path"))}
    }
    else { logger.info("gbdt模型路径不存在！");  Map("info"->"更新失败！","status"->"1","debug"->s"gbdt模型路径不存在！请检查！${input.gbdt_path}","gbdt_path"->model.current_model("gbdt_path"),"lr_path"-> model.current_model("lr_path"))}
  }

  /**
   * 查询api，查询当前模型的路径
   */
  get("/query_model"){
    logger.info("查询当前模型！")
    model.current_model
  }


}


