package stream.actor

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request
import akka.stream.actor.ActorPublisherMessage.Cancel
import scala.annotation.tailrec

object JobManager {
  val props: Props = Props[JobManager]
  
  final case class Job(payload: String)
  case object JobAccepted
  case object JobDenied
}

class JobManager extends ActorPublisher[JobManager.Job] {
  import JobManager._
  
  val maxBufferSize = 100
  var buf = Vector.empty[Job]
  
  def receive = {
    case job: Job if buf.size == maxBufferSize =>
      sender() ! JobDenied
    case job: Job =>
      sender() ! JobAccepted
      if (buf.isEmpty && totalDemand > 0) {
        onNext(job)
      }
      else {
        println("total demand " + totalDemand)
        buf :+= job
        println(buf)
        deliverBuf()
      }
    case Request(_) =>
      println("Requested with total demand " + totalDemand)
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }
  
  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */      
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}