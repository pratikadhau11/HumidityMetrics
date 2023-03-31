package com.metrics.humidity

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Sensor {
  sealed trait SensorCommand

  final case class RecordSensorHumidity(reading: Option[Int]) extends SensorCommand

  final case class SensorDataReading(replyTo: ActorRef[SensorFactory.SensorData]) extends SensorCommand

  final case class PassedReadings(replyTo: ActorRef[Int]) extends SensorCommand

  final case class FailedReadings(replyTo: ActorRef[Int]) extends SensorCommand

  class Sensor(name: String, humidityReadings: Seq[Option[Int]] = Seq()) {
    private def average: Option[Int] = {
      if (allValidReadings.isEmpty) {
        None
      } else
        Some(allValidReadings.sum / allValidReadings.length)
    }

    private def allValidReadings = humidityReadings.filter(_.isDefined).map(_.get)

    def behavior(): Behavior[SensorCommand] =
      Behaviors.setup[SensorCommand] { _ =>
        Behaviors.receiveMessage {
          case RecordSensorHumidity(reading) =>
            new Sensor(name, humidityReadings :+ reading).behavior()

          case SensorDataReading(replyTo) =>
            val sensorData = SensorFactory.SensorData(name,
              allValidReadings.sorted.headOption,
              allValidReadings.sorted.reverse.headOption,
              average)
            replyTo ! sensorData
            Behaviors.same

          case PassedReadings(replyTo) =>
            val passed = humidityReadings.count(_.isDefined)
            replyTo ! passed
            Behaviors.same

          case FailedReadings(replyTo) =>
            val failed = humidityReadings.count(_.isEmpty)
            replyTo ! failed
            Behaviors.same
        }
      }
  }
}
