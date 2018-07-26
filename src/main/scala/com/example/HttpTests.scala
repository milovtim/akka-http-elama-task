package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.stream.scaladsl
import akka.util.ByteString

import scala.concurrent.duration.DurationDouble
import scala.util.{Failure, Success}

object HttpTests extends App with HttpClient {

  override val system: ActorSystem = ActorSystem("HttpTest")

//  scaladsl.Source.fromFuture(http.singleRequest(req))
//    .map(resp => resp.entity.toStrict(3.seconds)).async
//    .runForeach(entity =>
//      entity
//      .flatMap(entity => entity.dataBytes.runFold(ByteString.empty) { case (acc, b) => acc ++ b })
//      .onComplete(bytes => println(bytes.getOrElse(ByteString.empty).map(_.toChar).mkString))
//    )
//  system.scheduler.scheduleOnce(5.seconds) { system.terminate() }

///*
    yesnoConcat
    .andThen {
      case Success(yn) => println(yn)
      case Failure(e) => println(e.getMessage)
    }
    .onComplete(_ => system.terminate())
//*/

//  parseCollectedResult
//  system.terminate()
}