package io.liquirium.bot

import io.liquirium.bot.helpers.BotOutputHelpers.output
import io.liquirium.core.helpers.{BasicTest, TestWithMocks}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CompoundOutputProcessorTest extends BasicTest with TestWithMocks {

  private def fakeProcessor(output: BotOutput, result: Boolean) = {
    val p = mock(classOf[BotOutputProcessor])
    p.processOutput(output) returns result
    p
  }

  private def apply[T](subProcessors: BotOutputProcessor*)(output: BotOutput): Boolean =
    CompoundOutputProcessor(subProcessors).processOutput(output)

  test("it returns false for any output when it has no sub providers") {
    apply()(output(3)) shouldBe false
  }

  test("it returns false when all sub provider return None") {
    apply(
      fakeProcessor(output(1), result = false),
      fakeProcessor(output(1), result = false)
    )(output(1)) shouldBe false
  }

  test("it returns true when one sub processor returns true") {
    apply(
      fakeProcessor(output(1), result = false),
      fakeProcessor(output(1), result = true)
    )(output(1)) shouldBe true
  }

  test("no more processors are tried as soon as the first processor returns true") {
    val probeProcessor = fakeProcessor(output(1), result = true)
    apply(
      fakeProcessor(output(1), result = false),
      fakeProcessor(output(1), result = true),
      probeProcessor,
    )(output(1))
    verify(probeProcessor, never).processOutput(output(1))
  }

}
