package stream.actor

import akka.actor.ActorRef
import akka.actor.Props
import akka.stream.actor.ActorSubscriber
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic
import akka.stream.actor.MaxInFlightRequestStrategy
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.actor.Actor

object WorkerPool {
  case class Msg(id: Int, replyTo: ActorRef)
  case class Work(id: Int)
  case class Reply(id: Int)
  case class Done(id: Int)
  
  val props: Props = Props[WorkerPool]
}

class WorkerPool extends ActorSubscriber {
  import WorkerPool._
  
  val maxQueueSize = 10
  var queue = Map.empty[Int, ActorRef]
  
  val router = {
    val routees = Vector.fill(3) {
      ActorRefRoutee(context.actorOf(Props[Worker]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def requestStrategy = new MaxInFlightRequestStrategy(max = maxQueueSize) {
    override def inFlightInternally: Int = queue.size
  }
  
  def receive = {
    case OnNext(Msg(id, replyTo)) =>
      queue += (id -> replyTo)
      assert(queue.size <= maxQueueSize, s"queued too many: ${queue.size}")
      router.route(Work(id), self)
    case Reply(id) =>
      queue(id) ! Done(id)
      queue -= id
  }
}

class Worker extends Actor {
  import WorkerPool._
  def receive = {
    case Work(id) =>
      println("Work Received for " + id)
      sender() ! Reply(id)
  }
}

