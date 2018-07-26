package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

final case class IntToIntResult(results: Map[Int, Int])
final case class IntToDoubleResult(results: Map[Int, Double])
final case class SummaryResult(impressions: Int, price: Double, spent: Double)


final case class YesNo(answer: String, forced: Boolean, image: String) {
  def +(other: YesNo): YesNo = {
    YesNo(answer + "/" + other.answer, forced && other.forced, image + "/" + other.image)
  }
}

trait MyJson extends DefaultJsonProtocol {
  implicit val yesNoFormat = jsonFormat3(YesNo)

  implicit val summaryResultFormat = jsonFormat3(SummaryResult)
  implicit val impressionsFormat = jsonFormat1(IntToIntResult)
  implicit val pricesFormat = jsonFormat1(IntToDoubleResult)
}


trait HttpClient extends MyJson with SprayJsonSupport {

  implicit val system: ActorSystem
  implicit lazy val dispatcher: ExecutionContextExecutor = system.dispatcher
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  lazy val http = Http(system)

  val req = HttpRequest(uri = "https://yesno.wtf/api")
  val uri = Uri("http://locahost")

  def yesno(request: HttpRequest): Future[YesNo] = {
    http.singleRequest(request)
      .flatMap(resp => resp.entity.dataBytes.runFold(ByteString.empty) { (acc, bt) => acc ++ bt })
      .map(bytes => bytes.utf8String.parseJson.convertTo[YesNo])
  }

  private def impressions(ids: Seq[Int]): Future[IntToIntResult] = {
    val r = HttpRequest(uri = uri.withPath(Path("/impressions")).withQuery(Uri.Query(ids.map(i => "id"-> s"$i"): _*)))
    http.singleRequest(r)
      .flatMap(resp => resp.entity.dataBytes.runFold(ByteString.empty) { (acc, bt) => acc ++ bt })
      .map(bytes => bytes.utf8String.parseJson.convertTo[IntToIntResult])
  }

  private def prices(ids: Seq[Int]): Future[IntToDoubleResult] = {
    val r = HttpRequest(uri = uri.withPath(Path("/prices")).withQuery(Uri.Query(ids.map(i => "id"-> s"$i"): _*)))
    http.singleRequest(r)
      .flatMap(resp => resp.entity.dataBytes.runFold(ByteString.empty) { (acc, bt) => acc ++ bt })
      .map(bytes => bytes.utf8String.parseJson.convertTo[IntToDoubleResult])
  }

  def yesnoConcat: Future[YesNo] = {
    val futures = List(yesno(req), yesno(req))
    Future.foldLeft(futures)(YesNo("", false, ""))(_ + _)
  }

  def requestDataAndMergeResults(campIds: Seq[Int]): Future[Map[Int, SummaryResult]] = {

    Future.failed(new RuntimeException)
  }


  def parseYesNo: Unit = {
    val data = """{"answer":"no","forced":false,"image":"https://yesno.wtf/assets/no/24-159febcfd655625c38c147b65e5be565.gif"}"""
    val yn = data.parseJson.convertTo[YesNo]
    println(yn)
  }

  def parseCollectedResult: Unit = {
    val data = """{
  "1": {
    "impressions": 123,
    "price": 2.31,
    "spent": 8.83
  }
}"""

    val yn = data.parseJson.convertTo[Map[String, SummaryResult]]
    println(yn)
  }
}