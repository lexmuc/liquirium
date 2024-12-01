package io.liquirium.core.helpers.async

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSource
import org.scalatest.Assertions.fail

// Unsafe means not thread safe
class UnsafeTestSource[E]()(implicit system: ActorSystem) {

  private var probes : Seq[TestPublisher.Probe[E]] = Seq()

  val source = TestSource.probe[E].mapMaterializedValue { probe =>
    probes = probes :+ probe
    NotUsed
  }

  def expectRun(): TestPublisher.Probe[E] = probes.headOption match {
    case Some(p) => {
      probes = probes.tail
      p
    }
    case None => fail("Expected source run but it wasn't run")
  }

  def expectNoRun() = probes.headOption match {
    case Some(p) => fail("Expected source not to be run but it was")
    case None => ()
  }

}
