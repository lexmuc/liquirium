package io.liquirium.helpers

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, Terminated}
import org.scalatest.Assertions.fail

class SynchronousBehaviorProbe[T](testKit: ActorTestKit) {

  var probes: Seq[TestProbe[T]] = Seq[TestProbe[T]]()

  val behavior: Behavior[T] = Behaviors.setup { context =>
    val testProbe = testKit.createTestProbe[T]()
    probes = probes :+ testProbe
    context.watch(testProbe.ref)

    Behaviors.receiveMessage[T] {
      m => testProbe.ref ! m; Behaviors.same
    }.receiveSignal {
      case (ctx, Terminated(a)) if (a == testProbe.ref) => Behaviors.stopped
    }
  }

  def expectSpawn(): TestProbe[T] =
    if (probes.isEmpty) fail("Expected actor spawn but was not observed.")
    else {
      val p = probes.head
      probes = probes.tail
      p
    }

}
