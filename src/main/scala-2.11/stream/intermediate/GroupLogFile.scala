package stream.intermediate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import java.io.File
import akka.util.ByteString
import akka.stream.scaladsl.{ Source, Sink, Framing, FileIO }
import java.nio.file.Paths

object GroupLogFile {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()
    
    val logLevelPattern = """.*\[(DEBUG|INFO|WARN|ERROR)\].*""".r
    val logFile = "src/main/resources/logfile.txt"
    
    FileIO.fromPath(Paths.get(logFile))
      .via(Framing.delimiter(ByteString(System.lineSeparator()), maximumFrameLength = 512, allowTruncation = true))
      .map { _.utf8String }
      .map {
        case line @ logLevelPattern(level) => (level, line)
        case line @ other                  => ("OTHER", line)
      }
      .groupBy(5, _._1)
      .fold(("", List.empty[String])) {
        case ((_, list), (level, line)) => (level, line :: list)
      }
      .mapAsync(parallelism = 5) {
        case (level, groupList) =>
          Source(groupList.reverse).map { line => ByteString(line + "\n") }.runWith(FileIO.toPath(Paths.get(s"target/log-$level.txt")))
      }
      .mergeSubstreams
      .runWith(Sink.onComplete { _ =>
        system.terminate()
       })
  }
}