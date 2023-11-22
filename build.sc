import mill._, scalalib._

object liquirium extends SbtModule with ScalaModule {
  def scalaVersion = "2.12.12"
  val akkaVersion = "2.6.10"
  def ivyDeps = Agg(
    ivy"org.joda:joda-convert:1.8.3",
    ivy"com.typesafe.play::play-json:2.6.3",
    ivy"org.scalaj::scalaj-http:2.4.1",
    ivy"org.scalactic::scalactic:3.0.0",
    ivy"org.scalatest::scalatest:3.0.0",
    ivy"org.mockito:mockito-core:4.7.0",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.0",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.h2database:h2:2.2.222",
    ivy"com.typesafe.akka::akka-actor:${akkaVersion}",
    ivy"com.typesafe.akka::akka-actor-typed:${akkaVersion}",
    ivy"com.typesafe.akka::akka-stream:${akkaVersion}",
    ivy"com.typesafe.akka::akka-stream-typed:${akkaVersion}",
    ivy"com.typesafe.akka::akka-stream-testkit:${akkaVersion}",
    ivy"com.typesafe.akka::akka-actor-testkit-typed:${akkaVersion}",
    ivy"com.typesafe.akka::akka-slf4j:${akkaVersion}",
    ivy"com.typesafe.akka::akka-http:10.2.2",
  )

  def sources = T.sources {
    super.sources() ++ Seq(PathRef(millSourcePath / os.up / os.up / "src" / "main" / "scala"))
  }

  object test extends ScalaTests with TestModule.ScalaTest {
//    println(millSourcePath / os.up)
    def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / os.up / os.up / "src" / "test" /"scala"))
    }
//    println(Seq(PathRef(millSourcePath / os.up / os.up/ "src" / "test" / "scala")))
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
  
}