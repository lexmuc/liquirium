import mill._, scalalib._, publish._

object liquirium extends SbtModule with ScalaModule with PublishModule {
  def scalaVersion = "2.12.12"

  val akkaVersion = "2.6.10"

  override def ivyDeps = Agg(
    ivy"org.joda:joda-convert:1.8.3",
    ivy"com.typesafe.play::play-json:2.6.3",
    ivy"org.scalaj::scalaj-http:2.4.1",
    ivy"org.scalactic::scalactic:3.0.0",
    ivy"org.scalatest::scalatest:3.0.0",
    ivy"org.mockito:mockito-core:4.7.0",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.0",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.h2database:h2:2.2.222",
    ivy"com.typesafe.akka::akka-actor:$akkaVersion",
    ivy"com.typesafe.akka::akka-actor-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-stream:$akkaVersion",
    ivy"com.typesafe.akka::akka-stream-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-stream-testkit:$akkaVersion",
    ivy"com.typesafe.akka::akka-actor-testkit-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-slf4j:$akkaVersion",
    ivy"com.typesafe.akka::akka-http:10.2.2",
  )

  override def publishVersion = "0.1.1"

  override def mainClass: T[Option[String]] = Some("io.liquirium.bot.BotRunner")

  override def pomSettings = PomSettings(
    description = "Functional framework for automated trading.",
    organization = "io.liquirium",
    url = "https://github.com/lexmuc/liquirium",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(owner = "lexmuc", repo = "liquirium"),
    developers = Seq(Developer(id = "lexmuc", name = "Alexander Steinhoff", url = "https://github.com/lexmuc")),
  )

  object test extends ScalaTests with TestModule.ScalaTest

}