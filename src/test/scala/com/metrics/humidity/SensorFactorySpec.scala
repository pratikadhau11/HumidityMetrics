package com.metrics.humidity

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.testkit.TestKit
import akka.util.Timeout
import com.metrics.humidity.SensorFactory._
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt;

class SensorFactorySpec   extends TestKit(ActorSystem("SensorFactory")) with AnyFreeSpecLike with Matchers{
  val testKit                   = ActorTestKit()
  implicit val timeout: Timeout = Timeout(2.seconds)
  "Sensor Factory Actor" - {
    "should record readings and get all sensors data" in  {

      val ref = testKit.spawn(SensorFactory.behaviour())

      ref ! SensorReading("s1", Some(10))
      ref ! SensorReading("s2", Some(10))
      ref ! SensorReading("s1", Some(20))
      ref ! SensorReading("s2", None)
      ref ! SensorReading("s3", None)

      val probe = testKit.createTestProbe[Seq[SensorData]]("testProbe")

      ref ! GetAllSensorData(probe.ref)

      probe.expectMessage(Seq(SensorData("s1", Some(10), Some(20), Some(15)),SensorData("s2", Some(10), Some(10), Some(10)),
      SensorData("s3", None, None, None)))
    }

    "should get all processed data" in  {
      val ref = testKit.spawn(SensorFactory.behaviour())

      ref ! SensorReading("s1", Some(10))
      ref ! SensorReading("s2", Some(10))
      ref ! SensorReading("s1", Some(20))
      ref ! SensorReading("s2", None)
      ref ! SensorReading("s3", None)

      val totalProcessedProbe = testKit.createTestProbe[Int]("totalProcessedProbe")
      ref ! TotalProcessed(totalProcessedProbe.ref)
      totalProcessedProbe.expectMessage(3)
    }

    "should get all failed data" in  {

      val ref = testKit.spawn(SensorFactory.behaviour())

      ref ! SensorReading("s1", Some(10))
      ref ! SensorReading("s2", Some(10))
      ref ! SensorReading("s1", Some(20))
      ref ! SensorReading("s2", None)
      ref ! SensorReading("s3", None)


      val totalFailedProbe = testKit.createTestProbe[Int]("totalFailedProbe")
      ref ! TotalFailures(totalFailedProbe.ref)
      totalFailedProbe.expectMessage(2)
    }

    "should sort sensor data in" - {
      val seq = Seq(SensorData("s1", Some(10), Some(80), Some(15)),SensorData("s2", Some(60), Some(65), Some(62)),
        SensorData("s3", None, None, None))
      "ascending order as per" - {
        val ordering = Ordering.Int
        "average reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeAverageReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s3")
          value(1).name shouldBe("s1")
          value(2).name shouldBe("s2")
        }

        "minimum reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeMinimumReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s3")
          value(1).name shouldBe("s1")
          value(2).name shouldBe("s2")
        }

        "maximum reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeMaximumReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s3")
          value(1).name shouldBe("s2")
          value(2).name shouldBe("s1")
        }
      }

      "descending order as per" - {
        val ordering = Ordering.Int.reverse
        "average reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeAverageReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s2")
          value(1).name shouldBe("s1")
          value(2).name shouldBe("s3")
        }

        "minimum reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeMinimumReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s2")
          value(1).name shouldBe("s1")
          value(2).name shouldBe("s3")
        }

        "maximum reading" in {
          val value = SensorFactory.sort(seq, ordering, (s: SensorData) => s.maybeMaximumReading.getOrElse(Int.MinValue))
          value(0).name shouldBe("s1")
          value(1).name shouldBe("s2")
          value(2).name shouldBe("s3")
        }
      }
    }
  }

}