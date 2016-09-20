package stream.windowing

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.event.Logging

import scala.concurrent.duration._
import scala.util.Random

/*
 * Time-based sliding windows of length 10 seconds and step 1 second.
 * Event-time derived from the data.
 * Watermark defined to be the timestamp of the newest event minus 5 seconds.
 * Emit only the final aggregate value
 */

object SlidingWindows {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    
    val f = Source
      .tick(initialDelay = 0 seconds, interval = 1 second, tick = "")
      .map { _ =>
        val now = System.currentTimeMillis()
        val delay = Random.nextInt(8)
        println(s"delay $delay")
        MyEvent(now - delay*1000L)
      }
    
    val consoleSink = Sink.foreach { println }
    f.to(consoleSink).run()
    
  }
  
  case class MyEvent(timestamp: Long)
}