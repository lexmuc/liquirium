package io.liquirium.bot

import io.liquirium.bot.BotInput.CompletedOperationRequest
import io.liquirium.core.TradeRequestId
import io.liquirium.eval.IncrementalFoldHelpers.{IncrementalMapEval, IncrementalEval}
import io.liquirium.eval.{Eval, IncrementalMap, IncrementalSeq}

object OpenOperationRequestsEval {

  type RequestStatesById = IncrementalMap[TradeRequestId, RequestState]

  case class RequestState(
    message: Option[OperationRequestMessage],
    completedRequest: Option[CompletedOperationRequest],
  ) {

    def record(message: OperationRequestMessage): RequestState = copy(
      message = Some(message)
    )

    def record(completedRequest: CompletedOperationRequest): RequestState = copy(
      completedRequest = Some(completedRequest)
    )

    def isOpen: Boolean = message.isDefined && completedRequest.isEmpty

  }

  def apply(
    allOperationRequestsEval: Eval[IncrementalSeq[OperationRequestMessage]],
    completedOperationRequestsEval: Eval[IncrementalSeq[CompletedOperationRequest]],
  ): Eval[Set[OperationRequestMessage]] = {

    val requestStatesById =
      allOperationRequestsEval.mergeFoldIncremental(completedOperationRequestsEval)(
        (_, _) => IncrementalMap.empty[TradeRequestId, RequestState]
      )(
        (statesById: RequestStatesById, orm: OperationRequestMessage) => {
          val newState = statesById.mapValue.get(orm.id) match {
            case None => RequestState(Some(orm), None)
            case Some(s) => s.record(orm)
          }
          statesById.update(orm.id, newState)
        }
      )(
        (statesById: RequestStatesById, cr: CompletedOperationRequest) => {
          val newState = statesById.mapValue.get(cr.requestMessage.id) match {
            case None => RequestState(None, Some(cr))
            case Some(s) => s.record(cr)
          }
          statesById.update(cr.requestMessage.id, newState)
        }
      )

    requestStatesById
      .filterValuesIncremental(_.isOpen)
      .map(_.mapValue.values.map(_.message.get))
      .map(_.toSet)
  }

}
