package io.liquirium.bot

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.bot.BotInput.{KeyboardInput, KeyboardInputEvent}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.async.AsyncTestWithControlledTime
import io.liquirium.eval.IncrementalSeq
import io.liquirium.helpers.FakeClock
import io.liquirium.util.helpers.FakeKeyboardInputReader

class KeyboardInputProviderTest extends AsyncTestWithControlledTime {

  val fakeInputReader = new FakeKeyboardInputReader()
  val clock: FakeClock = FakeClock(sec(0))
  val provider = new KeyboardInputProvider(fakeInputReader, clock, spawner, 10)

  implicit val system: ActorSystem[Nothing] = actorSystem

  private def getSource: Source[IncrementalSeq[KeyboardInputEvent], NotUsed] =
    provider.getInputUpdateStream(KeyboardInput).get

  test("it immediately emits the empty sequence of inputs when subscribed and starts the input reader") {
    val sinkProbe = getSource.runWith(TestSink.probe[IncrementalSeq[KeyboardInputEvent]])
    sinkProbe.requestNext() shouldEqual IncrementalSeq.empty[KeyboardInputEvent]
  }

  test("when new input lines are read, the history is extended and an update is issued in the stream") {
    val empty = IncrementalSeq.empty[KeyboardInputEvent]
    val sinkProbe = getSource.runWith(TestSink.probe[IncrementalSeq[KeyboardInputEvent]])
    sinkProbe.requestNext() shouldEqual empty
    clock.set(sec(10))
    fakeInputReader.fakeLine("fake1")
    val event1 = KeyboardInputEvent("fake1", sec(10))
    sinkProbe.requestNext() shouldEqual empty.inc(event1)
    clock.set(sec(20))
    fakeInputReader.fakeLine("fake2")
    val event2 = KeyboardInputEvent("fake2", sec(20))
    sinkProbe.requestNext() shouldEqual empty.inc(event1).inc(event2)
  }

}
