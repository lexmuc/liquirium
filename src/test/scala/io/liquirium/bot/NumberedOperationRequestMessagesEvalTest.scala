package io.liquirium.bot

import io.liquirium.bot.helpers.OperationRequestHelpers.{operationRequest, operationRequestMessage}
import io.liquirium.core.{CompoundOperationRequestId, OperationRequest, BotId}
import io.liquirium.eval.{Constant, Eval, IncrementalSeq}
import io.liquirium.eval.helpers.EvalTestWithIncrementalContext

class NumberedOperationRequestMessagesEvalTest extends EvalTestWithIncrementalContext[Seq[OperationRequestMessage]] {

  private val pastMessagesEval = fakeInputEval[IncrementalSeq[OperationRequestMessage]]
  private val newRequestsEval = fakeInputEval[Seq[OperationRequest]]

  private val botId = BotId("BOT123")
  private val otherBotId = BotId("OTHER")

  private def requestId(n: Int) = CompoundOperationRequestId(botId, n)

  private def otherRequestId(n: Int) = CompoundOperationRequestId(otherBotId, n)

  override protected val evalUnderTest: Eval[Seq[OperationRequestMessage]] =
    NumberedOperationRequestMessagesEval(
      botIdEval = Constant(botId),
      pastMessagesEval = pastMessagesEval,
      newRequestsEval = newRequestsEval,
    )

  private def fakePastMessages(mm: OperationRequestMessage*): Unit = {
    updateInput(pastMessagesEval, IncrementalSeq.apply(mm: _*))
  }

  private def fakeNewRequests(requests: OperationRequest*): Unit = {
    updateInput(newRequestsEval, requests)
  }

  test("output is empty when there are no new requests") {
    fakePastMessages(
      operationRequestMessage(requestId(1), operationRequest(123))
    )
    fakeNewRequests()
    eval() shouldEqual Seq()
  }

  test("the first request messages gets index 1") {
    fakePastMessages()
    fakeNewRequests(
      operationRequest(123),
    )
    eval() shouldEqual Seq(
      operationRequestMessage(requestId(1), operationRequest(123))
    )
  }

  test("messages are numbered consecutively starting from the index after the one in the last message") {
    fakePastMessages(
      operationRequestMessage(requestId(1), operationRequest(1)),
      operationRequestMessage(requestId(3), operationRequest(3)),
    )
    fakeNewRequests(
      operationRequest(123),
      operationRequest(234),
    )
    eval() shouldEqual Seq(
      operationRequestMessage(requestId(4), operationRequest(123)),
      operationRequestMessage(requestId(5), operationRequest(234)),
    )
  }

  test("messages from other bots are ignored") {
    fakePastMessages(
      operationRequestMessage(requestId(1), operationRequest(1)),
      operationRequestMessage(otherRequestId(2), operationRequest(2)),
    )
    fakeNewRequests(
      operationRequest(123),
    )
    eval() shouldEqual Seq(
      operationRequestMessage(requestId(2), operationRequest(123)),
    )
  }

}
