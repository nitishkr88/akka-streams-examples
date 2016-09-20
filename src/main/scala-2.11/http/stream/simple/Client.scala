package http.stream.simple

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer

/**
  * Created by Nitish Kumar on 20-09-2016.
  */
class Client extends App {
  private val port = 9001
  new Thread(new Server(port)).start()

  implicit val system = ActorSystem("Client")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val source = Uri(s"http://localhost:${port}/stream")
  val stream = Http().singleRequest(HttpRequest(uri = source)) flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      println(chunk.utf8String)
    }
  }

}
