package io.liquirium.bot.simulation

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest, unknownInputsException}
import io.liquirium.eval.{Input, UnknownInputsException}

import java.time.Instant

class SimulationInputUpdateStreamTest extends TestWithMocks {

  private var timedInputStreams: Map[Input[_], Stream[(Instant, Any)]] = Map()

  private val streamProvider: SingleInputUpdateStreamProvider = mock[SingleInputUpdateStreamProvider]

  private def updateStream(): SimulationInputUpdateStream = SimulationInputUpdateStream(
    timedInputStreams = timedInputStreams,
    singleInputStreamProvider = streamProvider,
  )

  private def fakeProvidedStream(i: Input[_], t: Instant)(elements: (Instant, Any)*) = {
    streamProvider.getInputStream(i, t) returns Some(elements.toStream)
  }

  private def fakeNoProvidedStream(i: Input[_], t: Instant) = {
    streamProvider.getInputStream(i, t) returns None
  }

  test("when there are no input streams the next input update is None") {
    timedInputStreams = Map()
    updateStream().nextInputUpdate shouldEqual None
  }

  test("when all streams are depleted the next input update is None") {
    timedInputStreams = Map(
      input(1) -> List().toStream,
      input(2) -> List().toStream,
    )
    updateStream().nextInputUpdate shouldEqual None
  }

  test("when there are streams the next input update consists of all inputs of the earliest heads of the streams") {
    timedInputStreams = Map(
      input(1) -> List(sec(2) -> 12, sec(3) -> 99).toStream,
      input(2) -> List(sec(1) -> 21, sec(3) -> 99).toStream,
      input(3) -> List(sec(1) -> 31).toStream,
    )
    updateStream().nextInputUpdate.get shouldEqual inputUpdate(
      input(2) -> 21,
      input(3) -> 31,
    )
  }

  test("there is still an update when part of the streams are depleted") {
    timedInputStreams = Map(
      input(1) -> List().toStream,
      input(2) -> List(sec(1) -> 21, sec(3) -> 99).toStream,
    )
    updateStream().nextInputUpdate.get shouldEqual inputUpdate(
      input(2) -> 21,
    )
  }

  test("after advancing the stream the next input update changes to the respectively next timestamp") {
    timedInputStreams = Map(
      input(1) -> List(sec(2) -> 12, sec(3) -> 99).toStream,
      input(2) -> List(sec(1) -> 21, sec(2) -> 22).toStream,
      input(3) -> List(sec(1) -> 31, sec(3) -> 33).toStream,
    )
    updateStream().advance.nextInputUpdate.get shouldEqual inputUpdate(
      input(1) -> 12,
      input(2) -> 22,
    )
  }

  test("an exception is thrown when trying to advance and there are no more updates") {
    timedInputStreams = Map(
      input(1) -> List().toStream,
    )
    an[Exception] shouldBe thrownBy(updateStream().advance)
  }

  test("an exception is thrown when trying to process inputs in an empty stream") {
    timedInputStreams = Map(
      input(1) -> List().toStream,
    )
    an[Exception] shouldBe thrownBy(updateStream().processInputRequest(inputRequest(input(1))))
  }

  test("the stream is extended via the input stream provider upon an input request (next time is start)") {
    timedInputStreams = Map(
      input(1) -> List(sec(10) -> 10).toStream,
    )
    val ir = inputRequest(input(2), input(3))
    fakeProvidedStream(input(2), sec(10))(sec(11) -> 211, sec(12) -> 212)
    fakeProvidedStream(input(3), sec(10))(sec(10) -> 310)
    updateStream().processInputRequest(ir) shouldEqual SimulationInputUpdateStream(
      timedInputStreams = Map(
        input(1) -> List(sec(10) -> 10).toStream,
        input(2) -> List(sec(11) -> 211, sec(12) -> 212).toStream,
        input(3) -> List(sec(10) -> 310).toStream,
      ),
      singleInputStreamProvider = streamProvider,
    )
  }

  test("an unknown input exception is raised when streams for some inputs cannot be supplied by the provider") {
    timedInputStreams = Map(
      input(1) -> List(sec(10) -> 10).toStream,
    )
    val ir = inputRequest(input(2), input(3), input(4))
    fakeProvidedStream(input(2), sec(10))(sec(11) -> 211)
    fakeNoProvidedStream(input(3), sec(10))
    fakeNoProvidedStream(input(4), sec(10))
    val thrownException = the [UnknownInputsException] thrownBy updateStream().processInputRequest(ir)
    thrownException shouldEqual unknownInputsException(input(3), input(4))
  }

}
