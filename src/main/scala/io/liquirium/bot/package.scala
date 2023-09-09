package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, Market, OrderConstraints}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}
import io.liquirium.util.store.CandleHistoryLoaderProvider

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

package object bot {

  def getSimpleOrderIntentConveyorEval(
    market: Market,
    orderConstraints: OrderConstraints,
    start: Instant,
    syncInterval: Duration,
  ): Eval[Seq[OrderIntent] => Iterable[BotOutput]] = {

    val orderStatesByIdEval = BasicOrderTrackingStateByIdEval(
      trades = InputEval(TradeHistoryInput(market, start)),
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

  def singleMarketStrategyBotFactoryForSimulation(
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    orderConstraints: OrderConstraints,
  )(
    implicit executionContext: ExecutionContext,
  ): SingleMarketBotFactory = new SingleMarketBotFactory {

    def makeBot(
      strategy: SingleMarketStrategy,
      market: Market,
      startTime: Instant,
      endTimeOption: Option[Instant],
      totalValue: BigDecimal,
    ): Future[SingleMarketStrategyBot] =
      for {
        p <- getInitialPrice(market, strategy.candleLength, startTime)
      } yield {
        println("initial price: " + p)
        val runConfiguration = SingleMarketBotRunConfiguration(
          market = market,
          startTime = startTime,
          endTimeOption = endTimeOption,
          initialResources = strategy.initialResources(
            totalQuoteValue = totalValue,
            initialPrice = p,
          ),
        )
        SingleMarketStrategyBot(
          strategy = strategy,
          runConfiguration = runConfiguration,
          orderIntentConveyorEval = io.liquirium.bot.getSimpleOrderIntentConveyorEval(
            market = market,
            orderConstraints = orderConstraints,
            start = startTime,
            syncInterval = Duration.ofSeconds(150),
          ),
        )
      }

    private def getInitialPrice(market: Market, candleLength: Duration, startTime: Instant): Future[BigDecimal] = {
      for {
        loader <- candleHistoryLoaderProvider.getHistoryLoader(market, candleLength)
        history <- loader.load(
          start = startTime.minusSeconds(60 * 60 * 12),
          time = startTime,
        )
      } yield history.lastPrice.get
    }

  }

}
