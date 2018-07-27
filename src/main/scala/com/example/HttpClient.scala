package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

final case class IntToIntResult(results: Map[String, Int])
final case class IntToDoubleResult(results: Map[String, Double])
final case class SummaryResult(impressions: Int, price: Double, spent: Double)


trait MyJson extends DefaultJsonProtocol {
  implicit val summaryResultFormat: RootJsonFormat[SummaryResult] = jsonFormat3(SummaryResult)
  implicit val impressionsFormat: RootJsonFormat[IntToIntResult] = jsonFormat1(IntToIntResult)
  implicit val pricesFormat: RootJsonFormat[IntToDoubleResult] = jsonFormat1(IntToDoubleResult)
}


trait HttpClient extends MyJson with SprayJsonSupport {

  implicit val system: ActorSystem
  implicit lazy val dispatcher: ExecutionContextExecutor = system.dispatcher
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  lazy val http = Http(system)
  val uri = Uri("http://localhost:9000")


  def impressions(ids: Seq[Int]): Future[IntToIntResult] = {
    val r = HttpRequest(uri = uri.withPath(Path("/impressions")).withQuery(Uri.Query(ids.map(i => "id"-> s"$i"): _*)))
    http.singleRequest(r)
      .flatMap(resp => {
        val entBytesFuture = resp.entity.dataBytes.runFold(ByteString.empty) { (acc, bt) => acc ++ bt }
        if (resp.status == StatusCodes.OK)
          entBytesFuture
        else {
//          println(s"imprs: status code: ${resp.status}")
          entBytesFuture.value.getOrElse(Success(ByteString.empty)) match {
            case Success(byteStr) => Future.failed(new RuntimeException(byteStr.utf8String))
            case Failure(ex) => Future.failed(ex)
          }
        }
      })
      .map(bytes => bytes.utf8String.parseJson.convertTo[IntToIntResult])
  }


  //ага, дублирование кода
  def prices(ids: Seq[Int]): Future[IntToDoubleResult] = {
    val r = HttpRequest(uri = uri.withPath(Path("/prices")).withQuery(Uri.Query(ids.map(i => "id"-> s"$i"): _*)))
    http.singleRequest(r)
      .flatMap(resp => {
        val entBytesFuture = resp.entity.dataBytes.runFold(ByteString.empty) { (acc, bt) => acc ++ bt }
        if (resp.status == StatusCodes.OK)
          entBytesFuture
        else {
//          println(s"prices: status code: ${resp.status}")
          entBytesFuture.value.getOrElse(Success(ByteString.empty)) match {
            case Success(byteStr) => Future.failed(new RuntimeException(byteStr.utf8String))
            case Failure(ex) => Future.failed(ex)
          }
        }
      })
      .map(bytes => bytes.utf8String.parseJson.convertTo[IntToDoubleResult])
  }


  def requestDataAndMergeResults(campIds: Seq[Int]): Future[Map[String, SummaryResult]] = {
    val combinedFuture = Future.foldLeft(List(impressions(campIds), prices(campIds)))(List[Any]()) { (lst, el) => lst :+ el}

    combinedFuture.map { lst =>
      val ordered = lst.sortWith { (a, b) => a.isInstanceOf[IntToIntResult] }
      val impr = ordered.head.asInstanceOf[IntToIntResult].results
      val prices = ordered(1).asInstanceOf[IntToDoubleResult].results
      (for (
        k <- impr.keys ++ prices.keys
        if impr.isDefinedAt(k) && prices.isDefinedAt(k)) //берём ключи, которые есть в обеих мапах
        yield (k, SummaryResult(impr(k), prices(k), impr(k) * prices(k)))
      ).toMap
    }
  }
}