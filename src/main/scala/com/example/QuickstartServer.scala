package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success}

object QuickstartServer extends HttpClient with App {

  override implicit val system: ActorSystem = ActorSystem("elama-task-server")
  implicit lazy val timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("yesno") {
      pathEnd {
        get {
          onComplete(yesno(req)) {
            case Success(yn) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, yn.image))
            case Failure(e) => complete(StatusCodes.InternalServerError -> e.getMessage)
          }
        }
      }
    }

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)
}
