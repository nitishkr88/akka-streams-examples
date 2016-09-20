package stream.intermediate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.SourceShape
import akka.NotUsed
import akka.stream.FlowShape

/*
 * CONSTRUCTING SOURCES, SINKS AND FLOWS FROM PARTIAL GRAPHS (expose a complex graph
 * as a simpler structure, such as a Source, Sink or Flow.)
 * 
 * Source is a partial graph with exactly one output, that is it returns a SourceShape.
 * Sink is a partial graph with exactly one input, that is it returns a SinkShape.
 * Flow is a partial graph with exactly one input and exactly one output, that is it returns a FlowShape.
 * 
 */
object PartialGraphs {
  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    
    /*
     * Source that zips together two numbers
     */
    val pairs: Source[(Int, Int), NotUsed] = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      
      // prepare graph elements
      val zip = builder.add(Zip[Int, Int]())
      val ints = Source(1 to 10)
      
      // connect the graph
      ints.filter { _ % 2 != 0 } ~> zip.in0
      ints.filter { _ % 2 == 0 } ~> zip.in1
      
      // expose port
      SourceShape(zip.out)      
    })
    
    // run
    val firstPair = pairs.runForeach(println)
    
    /*
     * Flow that pairs input with its toString
     */
    val pairUpWithToString: Flow[Int, (Int, String), NotUsed] =
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        
        // prepare graph elements
        val bcast = builder.add(Broadcast[Int](2))
        val zip = builder.add(Zip[Int, String]())
        
        // connect the graphs
        bcast.out(0).map { identity } ~> zip.in0
        bcast.out(1).map { _.toString } ~> zip.in1
        
        // expose ports
        FlowShape(bcast.in, zip.out)
      })
      
      // run
      pairUpWithToString.runWith(Source(List(1,2,3)), Sink.foreach { println })
    
  }
  
}