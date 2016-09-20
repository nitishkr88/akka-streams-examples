package stream.twitter

import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ClosedShape
import scala.concurrent.Future

object TwitterStream {
  
  def main(args: Array[String]): Unit = {  
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    
    val akka = Hashtag("#akka")
    val tweets: Source[Tweet, NotUsed] = ???
    val authors: Source[Author, NotUsed] =
      tweets
        .filter { _.hashtags.contains(akka) }
        .map { _.author }
    
        
    /*
     * Simple Streams
     */
    authors.runWith(Sink.foreach { println })
    // or
    authors.runForeach { println }
    
    /*
     * Flattening Sequences
     */
    val hastags: Source[Hashtag, NotUsed] =
      tweets.mapConcat { _.hashtags.toList }
    
    /*
     * Broadcasting a stream
     */
    val writeAuthors: Sink[Author, Unit] = ???
    val writeHashtags: Sink[Hashtag, Unit] = ???
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      
      val bcast = builder.add(Broadcast[Tweet](2))
      tweets ~> bcast.in
      bcast.out(0) ~> Flow[Tweet].map { _.author } ~> writeAuthors
      bcast.out(1) ~> Flow[Tweet].mapConcat { _.hashtags.toList } ~> writeHashtags
      
      ClosedShape      
    })
    // run the graph
    g.run()
    
    /*
     * Materialized values
     */
    val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map { _ => 1 }
    val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
    
    val counterGraph: RunnableGraph[Future[Int]] =
      tweets
        .via(count)
        .toMat(sumSink)(Keep.right)
        
    val sum: Future[Int] = counterGraph.run()
    sum.foreach { n => println(s"Total tweets processed: $n") }

  }

}

final case class Author(handle: String)
final case class Hashtag(name: String)
final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
}