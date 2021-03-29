package org.apache.spark.ml.gbtlr

import org.apache.spark.ml.classification.GBTClassificationModel
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.tree.DecisionTreeModelReadWrite._
import org.apache.spark.ml.tree.Node


class GBTLR(val gbtModel: GBTClassificationModel) extends Serializable {
  private val GBTMaxIter: Int = gbtModel.trees.length

  private val treeInfo: Seq[(Map[Int, NodeData], Array[Int])] = for (i <- 0 until GBTMaxIter) yield {
    val nodeMap=getLeafNodes(gbtModel.trees(i).rootNode)
    val leafNodeArray=nodeMap.values.toArray.filter(n=>n.leftChild == -1 && n.rightChild== -1).map(_.id)
    (nodeMap,leafNodeArray)
  }


  /**
   * 首先获取gbdt全部决策时的全部叶子节点id，然后对输入的原始特征数据进行判断，计算落入的叶子节点，然后根据两者放回叶子节点热编码
   * @param features
   * @return
   */
  def predictLeaf(features: Vector): Vector = {
    var newFeature = new Array[Double](0)
    for (i <- 0 until GBTMaxIter) {
      val nodeMap=treeInfo(i)._1
      val leafNodeArray=treeInfo(i)._2
      //为落入的叶子节点id。root节点默认为1
      val treePredict = predictModify(1,nodeMap,features)
      //构造一个长度为当前树叶子节点数的序列，默认全部元素为0
      val treeArray = new Array[Double]((gbtModel.trees(i).numNodes + 1) / 2)
      //indexOf为传入值的索引
      treeArray(leafNodeArray.indexOf(treePredict)) = 1
      newFeature = newFeature ++ treeArray
    }
    Vectors.dense(newFeature)
  }



  /**
   * 预测原始特征落入的单棵决策树的叶子节点id
   * @param nodeId
   * @param nodeMap
   * @param features
   * @return
   */
  private def predictModify(nodeId: Int,nodeMap:Map[Int, NodeData],features: Vector): Int = {
    //split是节点的一个分裂对象，定义了该次分裂的特征id、连续特征的分裂阈值、特征类型、分类特征的分类类别
    val currentNode=nodeMap(nodeId)
    val leftChild=currentNode.leftChild
    val rightChild=currentNode.rightChild
    if (leftChild == -1 && rightChild == -1) {
      //如果是叶子节点，返回节点id，最终只会返回符合全部规则的叶子节点id
      currentNode.id
    } else {
      val split = currentNode.split.getSplit
      if (split.shouldGoLeft(features)) {
        predictModify(leftChild,nodeMap,features)
      } else {
        predictModify(rightChild,nodeMap,features)
      }
    }
  }

  /**
   * 获取决策树全部节点nodedata
   * @param rootNode
   * @return
   */
  private def getLeafNodes(rootNode: Node): Map[Int,NodeData] = {
    val (nodeDatas, _) = NodeData.build(rootNode, 1)
    nodeDatas.map(n=>(n.id,n)).toMap
  }


}
