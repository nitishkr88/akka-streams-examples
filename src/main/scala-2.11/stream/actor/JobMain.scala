package stream.actor

import akka.stream.scaladsl._
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.actor.ActorRef

object JobMain {
  
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()
    
    val jobManagerSource: Source[JobManager.Job, ActorRef] = Source.actorPublisher[JobManager.Job](JobManager.props)
    
    val ref: ActorRef = Flow[JobManager.Job]
      .map(_.payload.toUpperCase())
      .map { elem => println(elem); elem }
      .to(Sink.ignore)
      .runWith[ActorRef](jobManagerSource)
    
    ref ! JobManager.Job("a")
    ref ! JobManager.Job("b")
    ref ! JobManager.Job("c")
  }
}