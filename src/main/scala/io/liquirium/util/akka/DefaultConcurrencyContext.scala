package io.liquirium.util.akka

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Props, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.scaladsl.adapter._
import akka.stream.OverflowStrategy

case object DefaultConcurrencyContext extends ConcurrencyContext {

  private val config = ConfigFactory.parseString(
    """
    akka {
       log-dead-letters = 10
       log-dead-letters-during-shutdown = off
       stdout-loglevel = "WARNING"
       loggers = ["akka.event.slf4j.Slf4jLogger"]
       loglevel = "WARNING"
       logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
   }

   h2-request-dispatcher {
     type = Dispatcher
     executor = "thread-pool-executor"
     thread-pool-executor {
       fixed-pool-size = 4
     }
     throughput = 1
   }

   akka.http.client.websocket.periodic-keep-alive-max-idle = 10 seconds
  """)

  override implicit lazy val actorSystem: akka.actor.typed.ActorSystem[SpawnProtocol.Command] =
    akka.actor.typed.ActorSystem(guardianBehavior(), "liquirium", config)

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val asyncHttpService = new AsyncHttpService()(actorSystem.toClassic)

  val spawner: ActorSpawner = new ActorSpawner {
    private val timeoutTime: FiniteDuration = 20.seconds
    implicit val timeout: Timeout = Timeout(timeoutTime)

    override def spawnAsync[T](behavior: Behavior[T]): Future[ActorRef[T]] =
      actorSystem.ask[ActorRef[T]](SpawnProtocol.Spawn(behavior, "", props = Props.empty, _))

    override def spawnAsync[T](behavior: Behavior[T], name: String): Future[ActorRef[T]] =
      actorSystem.ask[ActorRef[T]](SpawnProtocol.Spawn(behavior, name, props = Props.empty, _))

    override def executionContext: ExecutionContext = actorSystem.executionContext
  }

  private def guardianBehavior(): Behavior[SpawnProtocol.Command] = Behaviors.setup { _ => SpawnProtocol().behavior }

  val sourceQueueFactory: SourceQueueFactory = SourceQueueFactory.apply(spawner, 128, OverflowStrategy.fail)

}
