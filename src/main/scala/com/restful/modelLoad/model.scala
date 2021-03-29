package com.restful

import com.typesafe.config.ConfigFactory
import org.apache.spark.ml.classification.{GBTClassificationModel, LogisticRegressionModel}
import org.apache.spark.ml.gbtlr.Model_util
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable


/**
 * 模型加载实例。通过启动一个本地spark调取模型，然后通过gbtlr库中改写的方法预测
 */
object model {
  private val sparkConf: SparkConf =new SparkConf().setAppName("api").setMaster("local[*]").set("driver-memory","2g")

  var spark: SparkSession = {
    val sp=SparkSession.builder().config(sparkConf).getOrCreate()
    sp.sparkContext.setLogLevel("WARN")
    sp
  }

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  //加载模型
  val current_model: mutable.Map[String, String] ={
    val modelConf =ConfigFactory.load()
    val gbdt_path =modelConf.getString("model.gbdt_path")
    val lr_path = modelConf.getString("model.lr_path")
    mutable.Map("gbdt_path" -> gbdt_path,"lr_path" -> lr_path)
  }

  /**
   * 加载模型
   * @param gbdt_path 本地gbdt_path
   * @param lr_path 本地lr_path
   * @return
   */
  def load_model(gbdt_path:String,lr_path:String): Model_util ={
    if(spark.sparkContext.isStopped){
      logger.warn("spark异常关闭，现重启！")
      spark =SparkSession.builder().config(sparkConf).getOrCreate()
      spark.sparkContext.setLogLevel("WARN")
    }
    logger.info(s"开始加载gbdt模型:$gbdt_path")
    val gbdtModel = GBTClassificationModel.load("file://" + gbdt_path)
    logger.info(s"开始加载lr模型:$lr_path")
    val lrModel =LogisticRegressionModel.load("file://" + lr_path)
    logger.info("模型加载成功！")
    new Model_util(gbdtModel,lrModel)
  }

  /**
   * 存储当前模型的加载路径
   */
  var model_util: Model_util =load_model(current_model("gbdt_path"),current_model("lr_path"))

}

