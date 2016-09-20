package stream.backpressure

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import java.io.File
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.util.ByteString
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.Broadcast
import akka.stream.ClosedShape
import akka.stream.scaladsl.RunnableGraph
import scala.util.{ Failure, Success }
import akka.stream.scaladsl.Keep
import java.nio.file.Paths

object WritePrimes {
  
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher
    
    val maxRandomNumberSize = 1000000
    val primeSource =
      Source.fromIterator { () => Iterator.continually(ThreadLocalRandom.current().nextInt(maxRandomNumberSize)) }
        .filter(rnd => isPrime(rnd))
        .filter(prime => isPrime(prime + 2))
            
    val fileSink = FileIO.toPath(Paths.get("primes.txt"))
    
    val slowSink = Flow[Int]
        .map(i => { Thread.sleep(1000); ByteString(s"${i.toString}\n") })
        .toMat(fileSink)(Keep.right)
        
    val consoleSink = Sink.foreach[Int](println(_))
    
    val graph = GraphDSL.create(slowSink, consoleSink)((slow, _) => slow) { implicit builder =>
      (slow, console) =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        primeSource ~> broadcast ~> slow
                       broadcast ~> console
        ClosedShape
    }
        
    val materialized = RunnableGraph.fromGraph(graph).run()
    
    materialized.onComplete {
      case Success(_) =>
        system.terminate()
      case Failure(e) =>
        println(s"Failure: ${e.getMessage}")
        system.terminate()
    }
    
  }
  
  def isPrime(n: Int): Boolean = {
    if (n <= 1) false
    else if (n == 2) true
    else !(2 to (n-1)).exists { x => n % x == 0 }    
  }
}