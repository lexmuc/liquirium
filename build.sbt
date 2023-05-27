//name := "liquirium"
//
//version := "0.5"
//
//scalaVersion := "2.12.12"
//
//scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xmax-classfile-name", "78")
//
//val akkaVersion = "2.6.10"
//
//libraryDependencies ++= Seq(
//  "org.joda" % "joda-convert" % "1.8.3",
//  "com.typesafe.play" %% "play-json" % "2.6.3",
//  "org.scalaj" %% "scalaj-http" % "2.4.1",
//  "org.scalactic" %% "scalactic" % "3.0.0",
//  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
//  "org.mockito" % "mockito-core" % "4.7.0",
//  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
//  "ch.qos.logback" % "logback-classic" % "1.2.3",
//  "com.h2database" % "h2" % "1.4.197",
//
//  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
//  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
//  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
//  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
//  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
//  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
//  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
//  "com.typesafe.akka" %% "akka-http" % "10.2.2",
//)
//
//parallelExecution in Test := false

ThisBuild / scalaVersion := "2.12.12"
ThisBuild / organization := "com.example"

val akkaVersion = "2.6.10"

lazy val liquirium = (project in file("."))
  .settings(
    name := "Liquirium",
    libraryDependencies ++= Seq(
        "org.joda" % "joda-convert" % "1.8.3",
        "com.typesafe.play" %% "play-json" % "2.6.3",
        "org.scalaj" %% "scalaj-http" % "2.4.1",
        "org.scalactic" %% "scalactic" % "3.0.0",
        "org.scalatest" %% "scalatest" % "3.0.0" % "test",
        "org.mockito" % "mockito-core" % "4.7.0",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "com.h2database" % "h2" % "1.4.197",

        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % "10.2.2",
    ),
  )

lazy val liquiriumExamples = (project in file("examples"))
  .dependsOn(liquirium)
  .settings(
      name := "Liquirium examples",
  )