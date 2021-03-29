package com.restful

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import redis.clients.jedis.{HostAndPort, JedisCluster, JedisPoolConfig}
import scala.collection.JavaConverters._


/**
 * redis链接实例
 */
object RedisConnection {
  private val nodes=Array("192.168.0.1:7780","192.168.0.2:7880","192.168.0.3:7780")
    .map{node=>val Array(ip,port)=node.split(":")
      new HostAndPort(ip,port.toInt)}.toSet.asJava


  private val jedisPoolConfig = {
    val conf=new JedisPoolConfig()
    conf.setMaxIdle(20)
    conf.setMaxTotal(100)
    conf.setMinIdle(5)
    conf.setMaxWaitMillis(5000)
    conf.setTestOnBorrow(true)
    conf
  }

  private val mapper ={
    val objectMapper=new ObjectMapper()
    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
    objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
    objectMapper
  }

  val jedis = new JedisCluster(nodes,15000,5000,3,"abcd1234",jedisPoolConfig)

  def get(key:String): Option[util.HashMap[String, util.HashMap[String, String]]] ={
    Option(jedis.get(key)) match {
      case Some(value)=> Some(mapper.readValue(value,classOf[util.HashMap[String, util.HashMap[String, String]]]))
      case _=>None
    }
  }

}
