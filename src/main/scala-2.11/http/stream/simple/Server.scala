package http.stream.simple

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._

/**
  * Created by Nitish Kumar on 20-09-2016.
  */
class Server(port: Int) extends Runnable {
  override def run(): Unit = {
    implicit val system = ActorSystem("Server")
    implicit val materializer = ActorMaterializer()

    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/stream"), _, _, _) =>
        HttpResponse(entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`, Source.tick(0 seconds, 1 seconds, "test")))
    }

    val serverSource = Http().bind("localhost", port)

    serverSource.to(Sink.foreach { connection =>
      connection handleWithSyncHandler requestHandler
    }).run()

    /*serverSource.runForeach { connection =>
      connection handleWithSyncHandler requestHandler
    }*/

    println(s"Server online at http://localhost:$port/")
  }
}
