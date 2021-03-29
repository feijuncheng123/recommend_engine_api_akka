package org.apache.spark.ml.gbtlr

import org.apache.spark.ml.classification.{GBTClassificationModel, LogisticRegressionModel}
import org.apache.spark.ml.linalg.Vector

class Model_util(val gbtModel: GBTClassificationModel, val lrModel:LogisticRegressionModel) extends java.io.Serializable {
  private val lr=new LR_util(lrModel)

  private val gbdtlr=new GBTLR(gbtModel)

  def predictPerformance(features:Vector): Map[String, Double] ={
    val gbtFeatures = gbdtlr.predictLeaf(features)
    lr.predict(gbtFeatures)
  }


}
