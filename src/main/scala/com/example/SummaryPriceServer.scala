package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers

import scala.util.{Failure, Success}



object SummaryPriceServer extends HttpApp with App with HttpClient {

  override implicit val system: ActorSystem = ActorSystem("elama-task-server")

  override lazy val routes: Route =
    pathSingleSlash {
      pathEnd {
        get {
          parameters('id.as(PredefinedFromStringUnmarshallers.intFromStringUnmarshaller).*) { ids =>
            onComplete(requestDataAndMergeResults(ids.toSeq)) {
              case Success(res) => complete(res)
              case Failure(e) => complete(StatusCodes.InternalServerError -> e.getMessage)
            }
          }
        }
      }
    }

  startServer("localhost", 8080)
}
