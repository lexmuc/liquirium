package io.liquirium.bot

import io.liquirium.core.{BotId, CompoundOperationRequestId}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, IncrementalSeq}

object NextRequestIdsEval {

  def apply(
    botIdEval: Eval[BotId],
    pastMessagesEval: Eval[IncrementalSeq[BotOutput]],
  ): Eval[Stream[CompoundOperationRequestId]] = {
    // We have to pull this out of the for comprehension in order for caching to work
    val operationRequestMessagesEval = pastMessagesEval.collectIncremental({ case orm: OperationRequestMessage => orm })
    for {
      botId <- botIdEval
      operationRequestMessages <- operationRequestMessagesEval
    } yield {
      val nextIndex =
        operationRequestMessages
          .reverseIterator
          .filter(_.id.asInstanceOf[CompoundOperationRequestId].botId == botId)
          .take(1).toList.headOption
          .map(_.id.asInstanceOf[CompoundOperationRequestId].requestIndex + 1)
          .getOrElse(1)
      Stream.iterate(nextIndex)(_ + 1).map(index => CompoundOperationRequestId(botId, index))
    }
  }

}
