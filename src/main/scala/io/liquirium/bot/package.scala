package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, Market, OrderConstraints, TradeHistorySegment}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}

import java.time.{Duration, Instant}

package object bot {

  def getSimpleOrderIntentConveyorEval(
    market: Market,
    orderConstraints: OrderConstraints,
    start: Instant,
    syncInterval: Duration,
  ): Eval[Seq[OrderIntent] => Iterable[BotOutput]] = {

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
      currentTimeEval = InputEval(TimeInput(syncInterval)),
    )

    val hasOpenRequestsEval: Eval[Boolean] = OpenOperationRequestsEval(
      allOperationRequestsEval = InputEval(BotOutputHistory).collectIncremental {
        case orm: OperationRequestMessage => orm
      },
      completedOperationRequestsEval = InputEval(CompletedOperationRequestsInSession),
    ).map(_.nonEmpty)

    OrderIntentConveyor(
      market = market,
      orderConstraintsEval = Constant(orderConstraints),
      orderIntentSyncer = Constant(SimpleOrderIntentSyncer(OrderMatcher.ExactMatcher)),
      openOrdersEval = OpenOrdersBasedOnTrackingStates(orderStatesByIdEval),
      isInSyncEval = isInSyncEval,
      hasOpenRequestsEval = hasOpenRequestsEval,
      nextMessageIdsEval = NextRequestIdsEval(Constant(BotId("")), InputEval(BotOutputHistory)),
    )
  }

}
