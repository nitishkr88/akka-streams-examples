package stream.advanced

import akka.stream.scaladsl._
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.OverflowStrategy

object BalancingBuffer extends App {
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()
  
  def trailingDifference(offset: Int) = Flow.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    
    // prepare graph elements
    val bcast = builder.add(Broadcast[Int](2))
    val drop = builder.add(Flow[Int].drop(offset))
    val zip = builder.add(Zip[Int, Int])
    val diff = builder.add(Flow[(Int, Int)].map {
      case (num, trailing) => (num, num - trailing)
    })
    
    // balancing buffer
    val buffer = builder.add(Flow[Int].buffer(offset, overflowStrategy = OverflowStrategy.backpressure))
    
    // connect the graphs
    bcast ~> buffer ~> zip.in0
    bcast ~> drop   ~> zip.in1
                       zip.out ~> diff
                     
    FlowShape(bcast.in, diff.outlet)
    
  })
  
  Source(100 to 0 by -2).via(trailingDifference(7)).runWith(Sink.foreach(println))
  
}