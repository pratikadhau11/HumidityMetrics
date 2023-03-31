package com.metrics.humidity

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.testkit.TestKit
import akka.util.Timeout
import com.metrics.humidity.Sensor.{FailedReadings, PassedReadings, RecordSensorHumidity, Sensor, SensorDataReading}
import com.metrics.humidity.SensorFactory.SensorData
import org.scalatest.freespec.AnyFreeSpecLike

import scala.concurrent.duration.DurationInt;

class SensorSpec extends TestKit(ActorSystem("Sensor")) with AnyFreeSpecLike {
  val testKit                   = ActorTestKit()
  implicit val timeout: Timeout = Timeout(2.seconds)

  "Sensor Actor" - {
    "should send readings minimum, maximum, average" in  {
      val sensorName = "s1"
      val actorRef = testKit.spawn(new Sensor(sensorName).behavior())

      val minimumReading = Some(4)
      val maximumReading = Some(6)

      actorRef ! RecordSensorHumidity(minimumReading)
      actorRef ! RecordSensorHumidity(maximumReading)
      actorRef ! RecordSensorHumidity(None)

      val probe = testKit.createTestProbe[SensorFactory.SensorData]("testProbe")

      actorRef ! SensorDataReading(probe.ref)

      probe.expectMessage(SensorData(sensorName, minimumReading, maximumReading, Some(5)))
    }

    "should send readings minimum, maximum, average as None if all readings are None" in  {
      val sensorName = "s1"
      val actorRef = testKit.spawn(new Sensor(sensorName).behavior())


      actorRef ! RecordSensorHumidity(None)
      actorRef ! RecordSensorHumidity(None)
      actorRef ! RecordSensorHumidity(None)

      val probe = testKit.createTestProbe[SensorFactory.SensorData]("testProbe")

      actorRef ! SensorDataReading(probe.ref)

      probe.expectMessage(SensorData(sensorName, None, None, None))
    }

    "should calculate all processed readings" in  {
      val sensorName = "s1"
      val actorRef = testKit.spawn(new Sensor(sensorName).behavior())

      val minimumReading = Some(4)
      val maximumReading = Some(6)

      actorRef ! RecordSensorHumidity(minimumReading)
      actorRef ! RecordSensorHumidity(maximumReading)
      actorRef ! RecordSensorHumidity(None)

      val probe = testKit.createTestProbe[Int]("testProbe")

      actorRef ! PassedReadings(probe.ref)

      probe.expectMessage(2)
    }

    "should calculate all failed readings" in  {
      val sensorName = "s1"
      val actorRef = testKit.spawn(new Sensor(sensorName).behavior())

      val minimumReading = Some(4)
      val maximumReading = Some(6)

      actorRef ! RecordSensorHumidity(minimumReading)
      actorRef ! RecordSensorHumidity(maximumReading)
      actorRef ! RecordSensorHumidity(None)

      val probe = testKit.createTestProbe[Int]("testProbe")

      actorRef ! FailedReadings(probe.ref)

      probe.expectMessage(1)
    }
  }

}