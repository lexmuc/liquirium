package io.liquirium

import io.liquirium.bot.BotInput.{BotOutputHistory, CompletedOperationRequestsInSession, OrderSnapshotHistoryInput, TimeInput, TradeHistoryInput}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.orderTracking.{BasicOrderTrackingStateByIdEval, IsInSyncEval, OpenOrdersBasedOnTrackingStates, SuccessfulTradeRequestEvents}
import io.liquirium.core.{BotId, CompoundOperationRequestId, Market, OrderConstraints, TradeHistorySegment}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}

import java.time.{Duration, Instant}

package object bot {

  def getSimpleOrderIntentConveyor(
    market: Market,
    orderIntentsEval: Eval[Seq[OrderIntent]],
    orderConstraints: OrderConstraints,
    simulationMode: Boolean,
    start: Instant,
    timeInputForSync: TimeInput,
  ): Eval[Iterable[BotOutput]] = {
    val botId = BotId("")

    // #TODO make separate, tested eval
    val nextMessageIdsEval = InputEval(BotOutputHistory) map {
      rr =>
        val x = rr.reverseIterator.collect { case orm: OperationRequestMessage => orm.id }
        val lastIndex = if (x.isEmpty) 0 else x.next().asInstanceOf[CompoundOperationRequestId].requestIndex
        Stream.iterate(lastIndex + 1)(_ + 1).map(x => CompoundOperationRequestId(botId, x))
    }

    val tradeHistoryEval: Eval[TradeHistorySegment] =
      InputEval(TradeHistoryInput(market, if (simulationMode) Instant.ofEpochSecond(0) else start))

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
      nextMessageIdsEval = nextMessageIdsEval,
    )
  }

}
