import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "HumidityMetrics",
    mainClass := Some("com.metrics.humidity.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
      "org.scalatest" %% "scalatest" % "3.1.4" % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.19" % Test,
    )
  )
