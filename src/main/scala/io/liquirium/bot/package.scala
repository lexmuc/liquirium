package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, CandleHistorySegment, Market, OrderConstraints, TradeHistorySegment}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}
import io.liquirium.util.store.CandleStoreProvider

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

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

  def simulationSingleMarketStrategyBotFactory(
    orderConstraints: OrderConstraints,
    candleStoreProvider: CandleStoreProvider,
  )(
    implicit executionContext: ExecutionContext,
  ): SingleMarketBotFactory = new SingleMarketBotFactory {

    def makeBot(
      strategy: SingleMarketStrategy,
      market: Market,
      startTime: Instant,
      endTime: Option[Instant],
      totalValue: BigDecimal,
    ): Future[SingleMarketStrategyBot] = {

      val initialResourcesFuture =
        getInitialPrice(market, strategy.candleLength, startTime) map { p =>
          strategy.initialResources(
            totalQuoteValue = totalValue,
            initialPrice = p,
          )
        }

      initialResourcesFuture map { initialResources =>
        SingleMarketStrategyBot(
          market = market,
          startTime = startTime,
          initialResources = initialResources,
          strategy = strategy,
          orderIntentConveyorEval = io.liquirium.bot.getSimpleOrderIntentConveyorEval(
            market = market,
            orderConstraints = orderConstraints,
            start = startTime,
            syncInterval = Duration.ofSeconds(150),
          ),
        )
      }
    }

    private def getInitialPrice(market: Market, candleLength: Duration, startTime: Instant): Future[BigDecimal] = {
      val store = candleStoreProvider.getStore(market, candleLength)
      val candlesFuture = store.get(
        from = Some(startTime.minusSeconds(60 * 60 * 12)),
        until = Some(startTime),
      )
      candlesFuture.map(cc => CandleHistorySegment.fromCandles(cc).lastPrice.get)
    }

  }

}
