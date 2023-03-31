package com.metrics.humidity

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.util.Timeout
import com.metrics.humidity.SensorFactory.Command

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source


object Main extends App {
  val system: ActorSystem[Command] =
    ActorSystem(SensorFactory.behaviour(), "HumidityMetrics")
  implicit val scheduler: Scheduler = system.scheduler
  implicit val context: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout = Timeout.apply(4, TimeUnit.SECONDS)

  import SensorFactory._

  def getListOfFiles(directoryName: String): Seq[Source] = {
    val directory = new File(directoryName)
    if (directory.exists && directory.isDirectory)
      directory.list().map(name => Source.fromFile(s"${directory.getAbsolutePath}/$name"))
    else
      Seq.empty
  }

  val map: scala.collection.mutable.Map[String, scala.collection.mutable.Seq[Option[Int]]] = scala.collection.mutable.Map()
  private val files: Seq[Source] = getListOfFiles("src/main/resources/data")
  private val unit: Unit = files.flatMap(_.getLines().toSeq.tail)
    .map(_.split(","))
    .map(array => (array(0), array(1).toIntOption)).foreach {
    case (name, maybeInt: Option[Int]) =>
      system ! SensorReading(name, maybeInt)
  }

  println("Num of processed files: " + files.length)

  system.ask(replyTo => TotalProcessed(replyTo)) map { totalProcessed =>
    println("Num of processed measurements: " + totalProcessed)
  } recoverWith {
        case e: Exception => println(e.toString)
          Future()
  }

  system.ask(replyTo => TotalFailures(replyTo)) map { totalFailed =>
    println("Num of failed measurements: " + totalFailed)
  }

  system.ask(replyTo => GetAllSensorData(replyTo)) map {
    sensorDataList => {
      println("sensor-name,min,average,max")
      val orderedSensorData = sort(
        sensorDataList,
        Ordering.Int.reverse,
        (sensorData: SensorData) => sensorData.maybeMaximumReading.getOrElse(Int.MinValue))
      orderedSensorData.foreach(println)
    }
      system.terminate()
  } recoverWith{
    case e: Exception => println(e.toString)
      system.terminate()
      Future()
  }


}



