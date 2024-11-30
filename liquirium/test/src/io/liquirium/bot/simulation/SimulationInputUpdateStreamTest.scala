package io.liquirium.bot.simulation

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest, unknownInputsException}
import io.liquirium.eval.{Input, UnknownInputsException}
import io.liquirium.util.TimePeriod
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.{a, an, convertToAnyShouldWrapper, the, thrownBy}

import java.time.Instant

class SimulationInputUpdateStreamTest extends TestWithMocks {

  private val streamProvider: SingleInputUpdateStreamProvider = mock(classOf[SingleInputUpdateStreamProvider])

  private def stream(start: Instant, end: Instant) = SimulationInputUpdateStream(
    period = TimePeriod(start, end),
    singleInputStreamProvider = streamProvider,
  )

  private def fakeProvidedStream(i: Input[_], start: Instant, end: Instant)(elements: (Instant, Any)*) = {
    streamProvider.getInputStream(i, start = start, end = end) returns Some(elements.toStream)
  }

  private def fakeNoProvidedStream(i: Input[_], start: Instant, end: Instant) = {
    streamProvider.getInputStream(i, start = start, end = end) returns None
  }

  test("when the stream has just been created it has a current input update that is is empty") {
    stream(start = sec(10), end = sec(100)).currentInputUpdate shouldEqual Some(inputUpdate())
  }

  test("a new stream may be advanced once then it is depleted") {
    val s = stream(start = sec(10), end = sec(100))
    s.advance.currentInputUpdate shouldBe None
  }

  test("the current input update of a new stream is updated with the first update of a merged stream") {
    fakeProvidedStream(input(1), start = sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(11) -> 111,
    )
    stream(start = sec(10), end = sec(100))
      .processInputRequest(inputRequest(input(1)))
      .currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 110,
    ))
  }

  test("a stream based on a single input stream contains all the updates of the stream") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(11) -> 111,
      sec(12) -> 112,
    )
    val s =
      stream(start = sec(10), end = sec(100))
        .processInputRequest(inputRequest(input(1)))
    s.currentInputUpdate shouldEqual Some(inputUpdate(input(1) -> 110))
    s.advance.currentInputUpdate shouldEqual Some(inputUpdate(input(1) -> 111))
    s.advance.advance.currentInputUpdate shouldEqual Some(inputUpdate(input(1) -> 112))
  }

  test("when several input streams are requested their updates are combined or interleaved respectively") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(12) -> 112,
    )
    fakeProvidedStream(input(2), sec(10), end = sec(100))(
      sec(10) -> 210,
      sec(11) -> 211,
      sec(12) -> 212,
    )
    val s = stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1), input(2)))
    s.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 110,
      input(2) -> 210,
    ))
    s.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(2) -> 211,
    ))
    s.advance.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 112,
      input(2) -> 212,
    ))
  }

  test("when the last stream ends the current input update is none") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(12) -> 112,
    )
    fakeProvidedStream(input(2), sec(10), end = sec(100))(
      sec(10) -> 210,
      sec(13) -> 213,
    )
    val s = stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1), input(2)))
    s.advance.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(2) -> 213,
    ))
    s.advance.advance.advance.currentInputUpdate shouldBe None
  }

  test("new input streams are properly merged in an advanced stream from the respective time") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(11) -> 111,
      sec(12) -> 112,
    )
    fakeProvidedStream(input(2), sec(11), end = sec(100))(
      sec(11) -> 211,
      sec(13) -> 213,
    )
    fakeProvidedStream(input(3), sec(11), end = sec(100))(
      sec(11) -> 311,
      sec(12) -> 312,
      sec(13) -> 313,
    )
    val s0 =
      stream(start = sec(10), end = sec(100))
        .processInputRequest(inputRequest(input(1)))
        .advance
    val s = s0.processInputRequest(inputRequest(input(2), input(3)))
    s.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 111,
      input(2) -> 211,
      input(3) -> 311,
    ))
    s.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 112,
      input(3) -> 312,
    ))
    s.advance.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(2) -> 213,
      input(3) -> 313,
    ))
    s.advance.advance.advance.currentInputUpdate shouldBe None
  }

  test("when all streams are depleted an exception is thrown when trying to advance the stream further") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(12) -> 112,
    )
    fakeProvidedStream(input(2), sec(10), end = sec(100))(
      sec(10) -> 210,
    )
    val s = stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1), input(2)))
    val depletedStream = s.advance.advance
    a[RuntimeException] shouldBe thrownBy(depletedStream.advance)
  }

  test("an unknown input exception is raised when streams for some inputs cannot be supplied by the provider") {
    val ir = inputRequest(input(2), input(3), input(4))
    fakeProvidedStream(input(2), sec(10), end = sec(100))(sec(11) -> 211)
    fakeNoProvidedStream(input(3), sec(10), end = sec(100))
    fakeNoProvidedStream(input(4), sec(10), end = sec(100))
    val s = stream(start = sec(10), end = sec(100))
    val thrownException = the[UnknownInputsException] thrownBy s.processInputRequest(ir)
    thrownException shouldEqual unknownInputsException(input(3), input(4))
  }

  test("an exception is thrown when trying to process inputs in a depleted stream") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
    )
    fakeProvidedStream(input(1), sec(11), end = sec(100))(
      sec(10) -> 210,
    )
    val s = stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1)))
    an[Exception] shouldBe thrownBy(s.advance.advance.processInputRequest(inputRequest(input(2))))
  }

  test("a runtime exception is thrown when the first merged stream does not start at the given start") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(11) -> 111,
    )
    a[RuntimeException] shouldBe thrownBy(
      stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1)))
    )
  }

  test("a runtime exception is thrown when a stream does not start at the current start of an advanced stream") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(11) -> 111,
    )
    fakeProvidedStream(input(2), sec(11), end = sec(100))(
      sec(12) -> 111,
    )
    val s =
      stream(start = sec(10), end = sec(100))
        .processInputRequest(inputRequest(input(1)))
        .advance
    a[RuntimeException] shouldBe thrownBy(
      s.processInputRequest(inputRequest(input(2)))
    )
  }

  test("a runtime exception is thrown when a merged stream is empty") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))()
    a[RuntimeException] shouldBe thrownBy(
      stream(start = sec(10), end = sec(100)).processInputRequest(inputRequest(input(1)))
    )
  }

  test("a stream with a single element is properly merged") {
    fakeProvidedStream(input(1), sec(10), end = sec(100))(
      sec(10) -> 110,
      sec(11) -> 111,
    )
    fakeProvidedStream(input(2), sec(10), end = sec(100))(
      sec(10) -> 210,
    )
    val s =
      stream(start = sec(10), end = sec(100))
        .processInputRequest(inputRequest(input(1), input(2)))
    s.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 110,
      input(2) -> 210,
    ))
    s.advance.currentInputUpdate shouldEqual Some(inputUpdate(
      input(1) -> 111,
    ))
    s.advance.advance.currentInputUpdate shouldBe None
  }

}
