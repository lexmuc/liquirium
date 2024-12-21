import mill._, scalalib._, publish._

trait LiquiriumModule extends ScalaModule {
  def scalaVersion = "2.13.15"
}

object liquirium extends LiquiriumModule with PublishModule {

  val akkaVersion = "2.6.10"
  val http4sVersion = "0.23.29"

  override def ivyDeps = Agg(
    ivy"org.joda:joda-convert:1.8.3",
    ivy"com.typesafe.play::play-json:2.9.4",
    ivy"org.scalaj::scalaj-http:2.4.2",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.h2database:h2:2.2.222",
    ivy"com.typesafe.akka::akka-actor:$akkaVersion",
    ivy"com.typesafe.akka::akka-actor-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-stream:$akkaVersion",
    ivy"com.typesafe.akka::akka-stream-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-slf4j:$akkaVersion",
    ivy"com.typesafe.akka::akka-http:10.2.2",
    ivy"org.typelevel::cats-effect:3.5.5",
    ivy"org.http4s::http4s-ember-client::$http4sVersion",
    ivy"org.http4s::http4s-dsl::$http4sVersion",
  )

  override def publishVersion = "0.2.1"

  override def pomSettings = PomSettings(
    description = "Functional framework for automated trading.",
    organization = "io.liquirium",
    url = "https://github.com/lexmuc/liquirium",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(owner = "lexmuc", repo = "liquirium"),
    developers = Seq(Developer(id = "lexmuc", name = "Alexander Steinhoff", url = "https://github.com/lexmuc")),
  )

  object test extends ScalaTests with TestModule.ScalaTest with PublishModule {
    override def ivyDeps = Agg(
      ivy"org.scalactic::scalactic:3.2.17",
      ivy"org.scalatest::scalatest:3.2.17",
      ivy"org.mockito:mockito-core:5.14.2",
      ivy"com.typesafe.akka::akka-stream-testkit:$akkaVersion",
      ivy"com.typesafe.akka::akka-actor-testkit-typed:$akkaVersion",
    )

    // this module is published to make the test helpers available in other projects
    override def pomSettings = PomSettings(
      description = "Liquirium test helpers.",
      organization = "io.liquirium",
      url = "https://github.com/lexmuc/liquirium",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github(owner = "lexmuc", repo = "liquirium"),
      developers = Seq(Developer(id = "lexmuc", name = "Alexander Steinhoff", url = "https://github.com/lexmuc")),
    )

    override def publishVersion = "0.2.1"

    override def artifactName: T[String] = "liquirium-test"

  }

}

object `liquirium-examples` extends LiquiriumModule {

  override def moduleDeps = Seq(liquirium)

  def runSimulation = T {
    runMain("io.liquirium.examples.simulation.RunSimulation")
  }

  def runTicker = T {
    runMain("io.liquirium.examples.ticker.CandleBasedPriceTicker")
  }

}