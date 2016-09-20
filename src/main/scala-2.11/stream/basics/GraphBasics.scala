package stream.basics

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.ClosedShape

object GraphBasics {
  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      
      val in = Source(1 to 10)
      val out = Sink.foreach { println }
      
      // prepare graph elements
      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))
      
      // create intermediary flows
      val f1, f2, f3, f4 = Flow[Int].map { _ + 10 }
      
      // connect the graph
      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
                  bcast ~> f4 ~> merge
                  
      ClosedShape
    })
    
    graph.run()
    
  }
  
}