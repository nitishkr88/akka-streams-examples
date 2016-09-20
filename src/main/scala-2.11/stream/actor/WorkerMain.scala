package stream.actor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.actor.Actor
import akka.actor.Props

object WorkerMain {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()
    
    val N = 100
    val masterActor = system.actorOf(WorkerMaster.props)
    val ref = Source(1 to N).map(WorkerPool.Msg(_, masterActor))
      .runWith(Sink.actorSubscriber(WorkerPool.props))      
  }
}

class WorkerMaster extends Actor {
  import WorkerPool._
  def receive = {
    case WorkerPool.Done(i) => {
      println("Done for "+ i )
    }
  }
}

object WorkerMaster {
  val props = Props[WorkerMaster]
}