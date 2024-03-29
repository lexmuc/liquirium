package io.liquirium.bot

import io.liquirium.core.{CompoundOperationRequestId, OperationRequest, BotId}
import io.liquirium.eval.{Eval, IncrementalSeq}

object NumberedOperationRequestMessagesEval {

  def apply(
    botIdEval: Eval[BotId],
    pastMessagesEval: Eval[IncrementalSeq[OperationRequestMessage]],
    newRequestsEval: Eval[Seq[OperationRequest]],
  ): Eval[Seq[OperationRequestMessage]] =
    for {
      botId <- botIdEval
      pastMessages <- pastMessagesEval
      newRequests <- newRequestsEval
    } yield {
      val nextIndex =
        pastMessages
          .reverseIterator
          .filter(_.id.asInstanceOf[CompoundOperationRequestId].botId == botId)
          .take(1).toList.headOption
          .map(_.id.asInstanceOf[CompoundOperationRequestId].requestIndex + 1)
          .getOrElse(1)

      newRequests.foldLeft((nextIndex, Seq[OperationRequestMessage]())) {
        case ((n, mm), r) =>
          val newMessages = mm :+ OperationRequestMessage(CompoundOperationRequestId(botId, n), r)
          (n + 1, newMessages)
      }._2
    }

}
