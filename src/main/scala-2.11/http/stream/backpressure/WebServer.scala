package http.stream.backpressure

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Random

/**
  * Created by Nitish Kumar on 20-09-2016.
  */
object WebServer {

  private val port = 9000

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    // re-usable source
    val numbers: Source[Int, NotUsed] = Source.fromIterator { () => Iterator.continually(Random.nextInt()) }

    val route =
      path("random") {
        get {
          complete(
            HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              // transform each number to a chunk of bytes
              numbers.map { n => ByteString(s"$n\n") }
            )
          )
        }
      }

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(handler = route, interface = "0.0.0.0", port = port)

    println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap { _.unbind() }
      .onComplete { _ => system.terminate() }
  }

}
