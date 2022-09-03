package io.liquirium.bot

import io.liquirium.bot.helpers.BotInputHelpers.botInput

class BotRunnerTest_Mixed extends BotRunnerTest {

  test("it immediately evaluates without setting new inputs and forwards trader outputs") {
    fakeOutputs(out(1), out(2))
    start()
    expectOutputs(out(1), out(2))
  }

  test("input requests are also forwarded immediately") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    expectInputRequests(botInput(1), botInput(2))
  }

  test("input streams are immediately subscribed to") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    expectInputRequests(botInput(1), botInput(2))
    inputSource1.expectRun()
    inputSource2.expectRun()
  }

  test("it evaluates again and forwards results when an input arrives, even if others are outstanding") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    inputSource2.expectRun()

    assumeUpdatedInputsForNextRun(botInput(1) -> 11)
    fakeOutputs(out(1))
    probe1.sendNext(11)
    expectOutputs(out(1))
  }

  test("when outputs are empty the output processor is not called") {
    fakeOutputs()
    start()
    verify(outputProcessor, never).processOutput(*)
  }

  test("past input updates are remembered i.e. the context is preserved") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    val probe2 = inputSource2.expectRun()

    assumeUpdatedInputsForNextRun(botInput(1) -> 11)
    fakeOutputs()
    probe1.sendNext(11)

    assumeUpdatedInputsForNextRun(botInput(2) -> 22)
    fakeOutputs(out(1))
    probe2.sendNext(22)
    expectOutputs(out(1))
  }

  test("a known input update also leads to another evaluation") {
    fakeInputRequests(botInput(1))
    start()
    val probe1 = inputSource1.expectRun()

    assumeUpdatedInputsForNextRun(botInput(1) -> 11)
    fakeOutputs()
    probe1.sendNext(11)

    assumeUpdatedInputsForNextRun(botInput(1) -> 111)
    fakeOutputs(out(1))
    probe1.sendNext(111)
    expectOutputs(out(1))
  }

  test("an input that has already been requested is not requested again") {
    fakeInputRequests(botInput(1), botInput(2))
    start()
    val probe1 = inputSource1.expectRun()
    expectInputRequests(botInput(1), botInput(2))

    assumeUpdatedInputsForNextRun(botInput(1) -> 1)
    fakeInputRequests(botInput(2), botInput(3))
    reset(inputProvider)

    probe1.sendNext(1)
    expectInputRequests(botInput(3))
    verifyNoMoreInteractions(inputProvider)
  }

}
