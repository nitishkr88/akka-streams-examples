package stream.basics

import akka.stream.scaladsl._
import akka.NotUsed
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.Done

object StreamBasics {
  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    
    /*
     * Source: A processing stage with exactly one output, emitting 
     * data elements whenever downstream processing
     * stages are ready to receive them.
     */
    val source: Source[Int, NotUsed] = Source(1 to 10)
    
    /*
     * Sink A processing stage with exactly one input, requesting and accepting 
     * data elements possibly slowing down
     * the upstream producer of elements
     */
    val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
    
    // materialize the flow, getting the Sinks materialized value
    val sum: Future[Int] = source.runWith(sink) // 55
    
    // Immutable processing stages
    source.map { _ => 0 } // has no effect on source, since it's immutable
    source.runWith(sink) // 55
    
    val zeroes = source.map { _ => 0 } // returns new Source[Int], with `map()` appended
    zeroes.runWith(sink) // 0
    
    /*
     * RunnableGraph: A Flow that has both ends “attached” to a Source and Sink respectively, and is ready to be run().
     */
    // connect the Source to the Sink, obtaining a RunnableGraph
    val runnable: RunnableGraph[Future[Int]] =
      Source(1 to 10)
        .toMat(sink)(Keep.right)
    
    // get the materialized value of the FoldSink
    val sum1: Future[Int] = runnable.run()
    
    /*
     * Defining sources, sinks and flows
     */
    // an empty source
    val source1: Source[Nothing, NotUsed] = Source.empty
    
    // Create a source from a single element
    val source2: Source[String, NotUsed] = Source.single("one element")
    
    // Create a source from an Iterable
    val source3: Source[Int, NotUsed] = Source(List(1,2,3))
    
    // Create a source from a Future
    val source4: Source[String, NotUsed] = Source.fromFuture(Future.successful("Hello"))
    
    
    // A Sink that consumes a stream without doing anything with the elements
    val sink1: Sink[Any, Future[Done]] = Sink.ignore
    
    // Sink that returns a Future as its materialized value,
    // containing the first element of the stream
    val sink2: Sink[Nothing, Future[Nothing]] = Sink.head
    
    // Sink that folds over the stream and returns a Future
    // of the final result as its materialized value
    val sink3: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
    
    // A Sink that executes a side-effecting call for every element of the stream
    val sink4: Sink[String, Future[Done]] = Sink.foreach[String] { println }
        
    /*
     *  Ways to wire up different parts of a stream
     */
    // Explicitly creating and wiring up a Source, Sink and Flow
    Source(1 to 6).via(Flow[Int].map(_ * 2)).to(Sink.foreach(println(_)))
    
    // Starting from a Source
    val source5 = Source(1 to 6).map(_ * 2)
    source5.to(Sink.foreach(println(_)))
    
    // Starting from a Sink
    val sink5: Sink[Int, NotUsed] = Flow[Int].map(_ * 2).to(Sink.foreach(println(_)))
    Source(1 to 6).to(sink5)
    
    // Broadcast to a sink inline
    val otherSink: Sink[Int, NotUsed] =
      Flow[Int].alsoTo(Sink.foreach(println(_))).to(Sink.ignore)
    Source(1 to 6).to(otherSink)
    
  }
  
}