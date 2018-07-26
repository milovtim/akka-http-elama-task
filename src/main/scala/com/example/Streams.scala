package com.example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}


object Streams extends App {

  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

//  hostLevelHttpClient
//  combineFutures
  some


  def some: Unit = {
    val src: Source[Int, NotUsed] = Source.single(1)
//    val src: Source[Int, NotUsed] = Source(1 to 5)
    val out = Sink.foreach(println)

//    Source.fromGraph()

    val flow = RunnableGraph.fromGraph(GraphDSL.create() { implicit bldr: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = bldr.add(Broadcast[Int](2))
      val merge = bldr.add(Merge[Int](2))

//      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
//      src ~> f1 ~>  broadcast ~> f2 ~> merge ~> f3 ~> out
//                    broadcast ~> f4 ~> merge

       src ~> broadcast ~> merge ~> out
              broadcast ~> merge
      ClosedShape
    })
    flow.run()
  }

  def combineFutures: Unit = {
    val src = Source(1 to 3)

    val agg = for (
      a <- src.runWith(Sink.foreach(_ => println(Thread.currentThread().getName)));
      b <- src.toMat(Sink.foreach(_ => println(Thread.currentThread().getName)))(Keep.right).run()
    ) yield (a ,b)

    agg.onComplete(_ => system.terminate())
  }

  def hostLevelHttpClient: Unit = {
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model._
    import akka.stream.scaladsl._
    import akka.stream.{OverflowStrategy, QueueOfferResult}



    val queueSize = 10

    val poolClientFlow = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]]("akka.io")
    val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] = Source
      .queue[(HttpRequest, Promise[HttpResponse])](queueSize, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
          case (Success(resp), p) => p.success(resp)
          case (Failure(e), p)    => p.failure(e)
        })
      )(Keep.left)
      .run()

    def queueRequest(request: HttpRequest): Future[HttpResponse] = {
      val responsePromise = Promise[HttpResponse]()
      queue
        .offer(request -> responsePromise)
        .flatMap {
          case QueueOfferResult.Enqueued    => responsePromise.future
          case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
          case QueueOfferResult.Failure(ex) => Future.failed(ex)
          case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
        }
    }

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json(10)
  }

  def steams: Unit = {
    val run: RunnableGraph[Future[Int]] = Source(1 to 100).toMat(Sink.fold[Int, Int](0)(_ + _))(Keep.right)
    val result = Await.result(run.run(), 1.seconds)

    run.run().andThen {
      case Success(x) => println(x)
      case Failure(exception) => println(exception)
    }

    system.terminate().andThen {
      case _ => println("System stops")
    }
  }
}