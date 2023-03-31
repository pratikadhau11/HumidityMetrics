package com.metrics.humidity

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import com.metrics.humidity.Sensor.{FailedReadings, RecordSensorHumidity, Sensor, SensorCommand, SensorDataReading}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContextExecutor, Future}

object SensorFactory {
  sealed trait Command

  final case class SensorData(name: String, maybeMinimumReading: Option[Int], maybeMaximumReading: Option[Int], maybeAverageReading: Option[Int]) {
    override def toString: String = {
      s"$name,${maybeMinimumReading.getOrElse("NaN")},${maybeAverageReading.getOrElse("NaN")},${maybeMaximumReading.getOrElse("NaN")}"
    }
  }

  final case class SensorReading(name: String, maybeReading: Option[Int]) extends Command

  final case class GetAllSensorData(replyTo: ActorRef[Seq[SensorData]]) extends Command

  final case class TotalFailures(replyTo: ActorRef[Int]) extends Command

  final case class TotalProcessed(replyTo: ActorRef[Int]) extends Command

  private def findActor(sensors: Seq[(String, ActorRef[SensorCommand])], name: String): Option[ActorRef[SensorCommand]] = {
    sensors.find(nameAndActor => nameAndActor._1 == name).map(_._2)
  }

  def behavior(sensors: Seq[(String, ActorRef[SensorCommand])] = Seq.empty): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      implicit val context: ExecutionContextExecutor = ctx.executionContext
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val timeout: Timeout = Timeout.apply(2, TimeUnit.SECONDS)
      Behaviors.stopped(postStop = () => {
        println(s" SensorFactory stopped")
      })

      Behaviors.receiveMessage {
        case SensorReading(name, maybeReading) =>
          findActor(sensors, name).fold {
            val actor: ActorRef[SensorCommand] = ctx.spawn(new Sensor(name).behavior(), name)
            actor ! RecordSensorHumidity(maybeReading)
            behavior(sensors.appended((name, actor)))
          } { actor =>
            actor ! RecordSensorHumidity(maybeReading)
            Behaviors.same
          }

        case GetAllSensorData(replyTo) =>
          val eventualSensorsData: Seq[Future[SensorData]] = sensors.map {
            case (_, actor) => actor.ask(replyTo => SensorDataReading(replyTo))
          }
          val future = Future.sequence(eventualSensorsData)
          future.map((sensorsData: Seq[SensorData]) => {
            replyTo ! sensorsData
          })
          Behaviors.same

        case TotalFailures(replyTo) =>
          Future.sequence(sensors.map {
            case (_, actor) => actor.ask(replyTo => FailedReadings(replyTo))
          }).map {
            println("completed future")
            replyTo ! _.sum
          }
          Behaviors.same

        case TotalProcessed(replyTo) =>
          Future.sequence(sensors.map {
            case (_, actor) => actor.ask(replyTo => FailedReadings(replyTo))
          }).map {
            println("completed future")
            replyTo ! _.sum
          }
          Behaviors.same
      }
    }
}
