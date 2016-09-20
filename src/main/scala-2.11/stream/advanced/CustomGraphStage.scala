package stream.advanced

import akka.stream.stage.GraphStage
import akka.stream._
import akka.stream.stage._
import akka.NotUsed
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.stream.scaladsl._
import akka.Done

object CustomGraphStage extends App {  
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()
  
  val printSink: Sink[Any, Future[Done]] = Sink.foreach(println)
  
  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource  
  val mySource: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  
  val result1: Future[Int] = mySource.take(10).runFold(0)( _ + _ )
  
  val mapFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(new Map[Int, Int](_ * 2))
  val filterFlow = Flow.fromGraph(new Filter[Int](_ % 2 == 0))
  val duplicatorFlow = Flow.fromGraph(new Duplicator[Int])
  
  mySource.take(10).via(mapFlow).runWith(Sink.foreach(println))
  // mySource.take(10).via(filterFlow).runWith(printSink)
  // mySource.take(10).via(duplicatorFlow).runWith(printSink)

}

/**
 * Simple Source
 */
class NumbersSource extends GraphStage[SourceShape[Int]] {
  // Define the (sole) output of this stage
  val out: Outlet[Int] = Outlet("NumbersSource")
  // Define the shape of this stage, which is SourceShape with the port defined above
  override val shape: SourceShape[Int] = SourceShape(out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      private var counter = 1
      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          push(out, counter)
          counter += 1
        }
      })
    }
  }
  
}

/**
 * Custom Linear Processing
 */
class Map[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {  
  val in = Inlet[A]("Map.in")
  val out = Outlet[B]("Map.out")
  
  override val shape: FlowShape[A, B] = FlowShape.of(in, out)
  
  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    
    new GraphStageLogic(shape) {
      
      setHandler(in, new InHandler {
        override def onPush() = {
          push(out, f(grab(in)))
        }
      })
      
      setHandler(out, new OutHandler{
        override def onPull() = {
          pull(in)
        }
      })
    }
  }
}

class Filter[A] (f: A => Boolean) extends GraphStage[FlowShape[A, A]] {  
  val in = Inlet[A]("Filter.in")
  val out = Outlet[A]("Filter.out")
  
  override val shape = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      
      setHandler(in, new InHandler {
        override def onPush() = {
          val input = grab(in)
          if(f(input)) push(out, input)
          else pull(in)
        }
      })
      
      setHandler(out, new OutHandler {
        override def onPull() = {
          pull(in)
        }
      })
      
    }
  }
}

class Duplicator[A] extends GraphStage[FlowShape[A, A]] {
  val in = Inlet[A]("Duplicator.in")
  val out = Outlet[A]("Duplicator.out")
  
  override def shape = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      var lastElem: Option[A] = None
      
      setHandler(in, new InHandler {
        override def onPush() = {
          val elem = grab(in)
          lastElem = Some(elem)
          push(out, elem)
        }
        override def onUpstreamFinish() = {
          if (lastElem.isDefined) emit(out, lastElem.get)
          complete(out)
        }
      })
      
      setHandler(out, new OutHandler {
        override def onPull() = {
          if (lastElem.isDefined) {
            push(out, lastElem.get)
            lastElem = None
          } else {
            pull(in)
          }
        }
      })
    }
  }
  
}


