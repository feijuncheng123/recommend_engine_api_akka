
import _root_.akka.actor.ActorSystem
import com.restful.{RedisConnection, model, rest_api}
import javax.servlet.ServletContext
import org.scalatra._
import org.slf4j.{Logger, LoggerFactory}

class ScalatraBootstrap extends LifeCycle {
  val logger: Logger = LoggerFactory.getLogger(getClass)


  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler
  implicit val executor = actorSystem.dispatcher


//  val refresh_model_task = new Runnable {
//    override def run(): Unit = {
//      logger.info("开始刷新模型！")
//      model.refresh_model()
//    }}

//  scheduler.schedule(
//    initialDelay = Duration(25-currentHour,HOURS),
//    interval = Duration(24, HOURS),
//    runnable = refresh_model_task)

  override def init(context: ServletContext) {
    //    context.addListener(new org.scalatra.servlet.ScalatraListener)
    context.mount(new rest_api(actorSystem), "/*")
//    context.mount(new TestServlet(spark,t), "/*")
  }

  override def destroy(context: ServletContext) {
    model.spark.stop()
    RedisConnection.jedis.close()
  }
}
