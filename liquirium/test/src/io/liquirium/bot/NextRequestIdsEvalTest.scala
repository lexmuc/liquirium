package io.liquirium.bot

import io.liquirium.bot.helpers.BotHelpers
import io.liquirium.bot.helpers.OperationRequestHelpers.{operationRequest, operationRequestMessage}
import io.liquirium.core.{BotId, CompoundOperationRequestId}
import io.liquirium.eval.helpers.EvalTestWithIncrementalContext
import io.liquirium.eval.{Constant, Eval, IncrementalSeq}

class NextRequestIdsEvalTest extends EvalTestWithIncrementalContext[Stream[CompoundOperationRequestId]] {

  private val botOutputHistoryEval = fakeInputEval[IncrementalSeq[BotOutput]]

  private val botId = BotId("BOT123")
  private val otherBotId = BotId("OTHER")

  private def requestId(n: Int) = CompoundOperationRequestId(botId, n)

  private def otherRequestId(n: Int) = CompoundOperationRequestId(otherBotId, n)

  override protected val evalUnderTest: Eval[Stream[CompoundOperationRequestId]] =
    NextRequestIdsEval(
      botIdEval = Constant(botId),
      pastMessagesEval = botOutputHistoryEval,
    )

  private def fakeOutputHistory(mm: BotOutput*): Unit = {
    updateInput(botOutputHistoryEval, IncrementalSeq.apply(mm: _*))
  }

  test("when there are no past messages the ids start at index 1 (with correct bot id)") {
    fakeOutputHistory()
    eval().take(3).toList shouldEqual Seq(
      requestId(1),
      requestId(2),
      requestId(3),
    )
  }

  test("messages are numbered consecutively starting from the index after the one in the last message") {
    fakeOutputHistory(
      operationRequestMessage(requestId(1), operationRequest(1)),
      operationRequestMessage(requestId(3), operationRequest(3)),
    )
    eval().take(3).toList shouldEqual Seq(
      requestId(4),
      requestId(5),
      requestId(6),
    )
  }

  test("messages from other bots and other outputs are ignored") {
    fakeOutputHistory(
      BotHelpers.botOutput(123),
      operationRequestMessage(requestId(1), operationRequest(1)),
      operationRequestMessage(otherRequestId(2), operationRequest(2)),
    )
    eval().take(3).toList shouldEqual Seq(
      requestId(2),
      requestId(3),
      requestId(4),
    )
  }

}
