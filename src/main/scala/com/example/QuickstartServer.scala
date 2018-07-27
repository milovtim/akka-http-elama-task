package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success}
import spray.json._
import com.example.MyJson



object QuickstartServer extends HttpClient with App {

  override implicit val system: ActorSystem = ActorSystem("elama-task-server")
  implicit lazy val timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathSingleSlash {
      pathEnd {
        get {
          onComplete(requestDataAndMergeResults(Seq(1,2,3))) {
            case Success(res) => complete(StatusCodes.OK -> HttpEntity(ContentTypes.`application/json`, res))
            case Failure(e) => complete(StatusCodes.InternalServerError -> e.getMessage)
          }
        }
      }
    }

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)
}
