package org.apache.spark.ml.gbtlr

import org.apache.spark.ml.linalg.{BLAS, DenseVector, SparseVector, Vector, Vectors}
import org.apache.spark.ml.classification.LogisticRegressionModel


private[spark] class LR_util(lrModel:LogisticRegressionModel) {
  private val _coefficients=lrModel.coefficients
  private val _intercept=lrModel.intercept

  val margin: Vector => Double = (features) => {
    BLAS.dot(features, _coefficients) + _intercept
  }

  protected def predictRaw(features: Vector): Vector = {
      val m = margin(features)
      Vectors.dense(-m, m)
  }

  protected def predictProbability(features: Vector): Vector = {
    val rawPreds = predictRaw(features)
    raw2probabilityInPlace(rawPreds)
  }

  def predict(features: Vector):Map[String,Double]={
    val probability=predictProbability(features.asInstanceOf[Vector])
    Map("probability(0)"-> probability(0),"probability(1)"->probability(1))
  }

  protected def raw2probabilityInPlace(rawPrediction: Vector): Vector = {
    rawPrediction match {
      case dv: DenseVector =>
        var i = 0
        val size = dv.size
        while (i < size) {
          dv.values(i) = 1.0 / (1.0 + math.exp(-dv.values(i)))
          i += 1
        }
        dv
      case sv: SparseVector =>
        throw new RuntimeException("Unexpected error in LogisticRegressionModel:" +
          " raw2probabilitiesInPlace encountered SparseVector")
    }
  }

}
