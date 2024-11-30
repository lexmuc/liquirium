package io.liquirium.bot

import io.liquirium.bot.BotInput.CompletedOperationRequest
import io.liquirium.bot.helpers.OperationRequestHelpers.{completedOperationRequest, operationRequestMessage}
import io.liquirium.eval.helpers.EvalTestWithIncrementalContext
import io.liquirium.eval.{Eval, IncrementalSeq}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OpenOperationRequestsEvalTest extends EvalTestWithIncrementalContext[Set[OperationRequestMessage]] {

  private val allRequestMessagesEval = fakeInputEval[IncrementalSeq[OperationRequestMessage]]
  private val completedRequestsEval = fakeInputEval[IncrementalSeq[CompletedOperationRequest]]

  override protected val evalUnderTest: Eval[Set[OperationRequestMessage]] =
    OpenOperationRequestsEval(allRequestMessagesEval, completedRequestsEval)

  private var allRequestMessages = IncrementalSeq.empty[OperationRequestMessage]
  private var completedRequests = IncrementalSeq.empty[CompletedOperationRequest]

  updateInput(allRequestMessagesEval, allRequestMessages)
  updateInput(completedRequestsEval, completedRequests)

  private def fakeNew(m: OperationRequestMessage): Unit = {
    allRequestMessages = allRequestMessages.inc(m)
    updateInput(allRequestMessagesEval, allRequestMessages)
  }

  private def fakeNew(cr: CompletedOperationRequest): Unit = {
    completedRequests = completedRequests.inc(cr)
    updateInput(completedRequestsEval, completedRequests)
  }

  test("when both sequences are empty the result is empty") {
    eval() shouldEqual Set()
  }

  test("when all requests are completed the result is empty") {
    fakeNew(operationRequestMessage(1))
    fakeNew(operationRequestMessage(2))
    fakeNew(completedOperationRequest(1))
    fakeNew(completedOperationRequest(2))
    eval() shouldEqual Set()
  }

  test("in general it yields all requests that are not yet answered") {
    fakeNew(operationRequestMessage(1))
    fakeNew(operationRequestMessage(2))
    fakeNew(operationRequestMessage(3))
    fakeNew(completedOperationRequest(2))
    eval() shouldEqual Set(
      operationRequestMessage(1),
      operationRequestMessage(3),
    )
    fakeNew(completedOperationRequest(3))
    eval() shouldEqual Set(
      operationRequestMessage(1),
    )
    fakeNew(completedOperationRequest(1))
    eval() shouldEqual Set()
  }

  test("completed requests may even appear before request messages (relevant for simulation)") {
    fakeNew(completedOperationRequest(1))
    eval() shouldEqual Set()
    fakeNew(operationRequestMessage(1))
    eval() shouldEqual Set()
  }

}
