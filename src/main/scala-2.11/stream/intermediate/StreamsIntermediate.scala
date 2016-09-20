package stream.intermediate

import akka.stream.scaladsl._
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.util.ByteString
import java.nio.file.Paths
import scala.concurrent.Future
import akka.stream.IOResult
import akka.stream.ThrottleMode
import scala.concurrent.duration._
import scala.BigInt
import scala.math.BigInt.int2bigInt

object StreamsIntermediate {
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    
    // simple source, emitting the integers 1 to 100
    val source: Source[Int, NotUsed] = Source(1 to 100)
    
    // emit first 100 natural numbers
    source.runForeach { println }
    
    // reusable pieces
    val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) => acc * next)
    val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("factorials.txt"))
    
    def lineSink(filename: String): Sink[String, Future[IOResult]] =
      Flow[String]
        .map { num => ByteString(s"$num\n") }
        .toMat(fileSink)(Keep.right)
    
    /*def lineSinkLeftMat(filename: String): Sink[String, NotUsed] =
      Flow[String]
        .map { num => ByteString(s"$num\n") }
        .toMat(fileSink)(Keep.left)*/
    
    // wire the pieces and run
    val results: Future[IOResult] =
      factorials
        .map { _.toString }
        .runWith(lineSink("factorials.txt"))
        
    // Time Based Processing and Stream Transformation
    /*
     * The secret that makes this work is that Akka Streams implicitly implement pervasive flow control, 
     * all combinators respect back-pressure. This allows the throttle combinator to signal to all its upstream sources of
     * data that it can only accept elements at a certain rate—when the incoming rate is higher than one per second the
     * throttle combinator will assert back-pressure upstream.
     */
    val done =
      factorials
        .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
        .throttle(elements = 1, per = 1 second, maximumBurst = 1, mode = ThrottleMode.shaping) // signals back-pressure
        .runForeach { println }
    
  }
}