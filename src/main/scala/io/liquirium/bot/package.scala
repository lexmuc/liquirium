package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, Market, OrderConstraints, TradeHistorySegment}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}

import java.time.{Duration, Instant}

package object bot {

  def getSimpleOrderIntentConveyor(
    market: Market,
    orderIntentsEval: Eval[Seq[OrderIntent]],
    orderConstraints: OrderConstraints,
    start: Instant,
    timeInputForSync: TimeInput,
  ): Eval[Iterable[BotOutput]] = {

    val tradeHistoryEval: Eval[TradeHistorySegment] =
      InputEval(TradeHistoryInput(market, start))

    val orderStatesByIdEval = BasicOrderTrackingStateByIdEval(
      trades = tradeHistoryEval,
      openOrdersHistory = InputEval(OrderSnapshotHistoryInput(market)),
      successfulOperations = SuccessfulTradeRequestEvents(market),
    )

    val isInSyncEval: Eval[Boolean] = IsInSyncEval(
      statesByIdEval = orderStatesByIdEval,
      maxSyncDurationEval = Constant(Duration.ofSeconds(60)),
      currentTimeEval = InputEval(timeInputForSync),
    )

    val hasOpenRequestsEval: Eval[Boolean] = OpenOperationRequestsEval(
      allOperationRequestsEval = InputEval(BotOutputHistory).collectIncremental {
        case orm: OperationRequestMessage => orm
      },
      completedOperationRequestsEval = InputEval(CompletedOperationRequestsInSession),
    ).map(_.nonEmpty)

    OrderIntentConveyor(
      market = market,
      orderIntentsEval = orderIntentsEval,
      orderConstraintsEval = Constant(orderConstraints),
      openOrdersEval = OpenOrdersBasedOnTrackingStates(orderStatesByIdEval),
      orderIntentSyncer = Constant(SimpleOrderIntentSyncer(OrderMatcher.ExactMatcher)),
      isInSyncEval = isInSyncEval,
      hasOpenRequestsEval = hasOpenRequestsEval,
      nextMessageIdsEval = NextRequestIdsEval(Constant(BotId("")), InputEval(BotOutputHistory)),
    )
  }

}
