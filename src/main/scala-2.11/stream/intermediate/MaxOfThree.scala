package stream.intermediate

import akka.stream.scaladsl._
import akka.stream.UniformFanInShape
import akka.stream.ClosedShape
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import scala.concurrent.duration._

 /*
  * CONSTRUCTING AND COMBINING PARTIAL GRAPHS
  * 
  * Provide with a specialized element that given 3 inputs will pick the greatest
  * int value of each zipped triple. We’ll want to expose 3 input ports (unconnected sources) and one output port
  * (unconnected sink).
  */

object MaxOfThree {
  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    
    val pickMaxOfThree = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      
      /*
       * ZipWith[A,B,...,Out] – (N inputs, 1 output) which takes a function of N inputs that given a value
       * for each input emits 1 output element
       */
      // prepare graph elements
      val zip1 = builder.add(ZipWith[Int, Int, Int](math.max _))
      val zip2 = builder.add(ZipWith[Int, Int, Int](math.max _))
      
      // connect the graph
      zip1.out ~> zip2.in0
      
      // Expose 3 input ports (unconnected sources) and one output port (unconnected sink)
      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)      
    }
    
    val resultSink = Sink.head[Int]
    
    val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit builder => sink =>
      import GraphDSL.Implicits._
      
      // importing the partial graph will return its shape (inlets & outlets)
      val pm3: UniformFanInShape[Int, Int] = builder.add(pickMaxOfThree)
      
      Source.single(100) ~> pm3.in(0)
      Source.single(200) ~> pm3.in(1)
      Source.single(300) ~> pm3.in(2)
      
      pm3.out ~> sink.in
      ClosedShape
    })
    
    val max: Future[Int] = g.run()
    max.onSuccess {
      case result => println(s"Max value: $result")
    }
    
  }
  
}