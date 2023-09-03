package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.Overfill
import io.liquirium.core.orderTracking.{BasicOrderTrackingState, OrderTrackingEvent}
import io.liquirium.util.AbsoluteQuantity

object OrderIsNotOverfilled extends ConsistencyRule {

  override def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    if (state.orderWithFullQuantity.isDefined) {
      val fullOrderQuantity = state.orderWithFullQuantity.get.fullQuantity
      state.cancellation match {
        case Some(OrderTrackingEvent.Cancel(_, _, Some(AbsoluteQuantity(q))))
          if state.totalTradeQuantity.abs > fullOrderQuantity.abs - q =>
          val maxFill =
            if (fullOrderQuantity.signum > 0) fullOrderQuantity - q
            else fullOrderQuantity + q
          Some(Overfill(state.tradeEvents.last, totalFill = state.totalTradeQuantity, maxFill = maxFill))
        case _ if state.totalTradeQuantity.abs > fullOrderQuantity.abs =>
          Some(Overfill(state.tradeEvents.last, totalFill = state.totalTradeQuantity, maxFill = fullOrderQuantity))
        case _ =>
          None
      }
    }
    else None
  }

}
