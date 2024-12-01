package io.liquirium.bot

import io.liquirium.bot.BotRunner.ShutdownReason
import io.liquirium.bot.helpers.BotInputHelpers.botInput
import io.liquirium.core.helpers.CoreHelpers.ex
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BotRunnerTest_FailureAndShutdown extends BotRunnerTest {

  test("it invokes the shutdown handler in case of an exception during evaluation (with the exception)") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeFailureForNextEvaluation(ex(123))
    probe1.sendNext(1)
    shutdownReason shouldEqual Some(ShutdownReason.EvaluationFailure(ex(123)))
  }

  test("after an evaluation exception all input streams are cancelled") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    val probe2 = inputSource2.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeFailureForNextEvaluation(ex(123))
    probe1.sendNext(1)
    probe1.expectCancellation()
    probe2.expectCancellation()
  }

  test("it invokes the shutdown handler when an unknown input is encountered") {
    fakeInputRequests(botInput(1), botInput(2))
    fakeNoInputSource(botInput(3))
    start()
    val probe1 = inputSource1.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeInputRequests(botInput(3))
    probe1.sendNext(1)
    shutdownReason shouldEqual Some(ShutdownReason.UnknownInput(botInput(3)))
  }

  test("other inputs are cancelled when an unknown input is encountered") {
    fakeInputRequests(botInput(1), botInput(2))
    fakeNoInputSource(botInput(3))
    start()
    val probe1 = inputSource1.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeInputRequests(botInput(3))
    probe1.sendNext(1)
    probe1.expectCancellation()
  }

  test("the shutdownHandler is invoked when an input stream fails") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()
    probe1.sendError(ex(123))
    shutdownReason shouldEqual Some(ShutdownReason.InputStreamFailure(botInput(1), ex(123)))
  }

  test("other input streams are cancelled when an input stream fails") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    val probe2 = inputSource2.expectRun()
    probe1.sendError(ex(123))
    probe2.expectCancellation()
  }

  test("the shutdown handler is invoked when an input stream completes") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()
    probe1.sendComplete()
    shutdownReason shouldEqual Some(ShutdownReason.InputStreamCompletion(botInput(1)))
  }

  test("other input streams are cancelled when an input stream completes") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    val probe2 = inputSource2.expectRun()
    probe1.sendComplete()
    probe2.expectCancellation()
  }

  test("the shutdown handler is invoked when an output cannot be processed") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeOutputs(out(1), out(2))
    outputProcessor.processOutput(out(2)) returns false
    probe1.sendNext(1)
    shutdownReason shouldEqual Some(ShutdownReason.UnprocessableOutput(out(2)))
  }

  test("input streams are cancelled when an output cannot be processed") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()
    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeOutputs(out(1), out(2))
    outputProcessor.processOutput(out(2)) returns false
    probe1.sendNext(1)
    probe1.expectCancellation()
  }

}
