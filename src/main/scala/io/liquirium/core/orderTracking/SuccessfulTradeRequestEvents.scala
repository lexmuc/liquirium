package io.liquirium.core.orderTracking

import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession}
import io.liquirium.bot.OperationRequestMessage
import io.liquirium.core.{CancelRequest, CancelRequestConfirmation, Market, OrderRequestConfirmation}
import io.liquirium.eval.IncrementalFoldHelpers._
import io.liquirium.eval.{Eval, IncrementalSeq, InputEval}


object SuccessfulTradeRequestEvents {

  def apply(market: Market): Eval[IncrementalSeq[OrderTrackingEvent.OperationEvent]] =
    InputEval(CompletedOperationRequestsInSession).collectIncremental {

      case CompletedOperationRequest(t, _, Right(OrderRequestConfirmation(Some(o), _))) if o.market == market =>
        OrderTrackingEvent.Creation(t, o)

      case CompletedOperationRequest(
      t,
      OperationRequestMessage(_, CancelRequest(m, oid)),
      Right(CancelRequestConfirmation(rest))
      ) if m == market =>
        OrderTrackingEvent.Cancel(t, oid, rest)

    }

}
