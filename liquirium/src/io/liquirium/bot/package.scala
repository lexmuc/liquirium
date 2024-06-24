package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, Market, OperationIntent, OrderConstraints}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}
import io.liquirium.util.TimePeriod
import io.liquirium.util.store.CandleHistoryLoaderProvider

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

package object bot {

  class ProductionOrderIntentConveyorFactory(
    syncInterval: Duration,
  ) extends OrderIntentConveyorFactory {
    def apply(
      market: Market,
      orderConstraints: OrderConstraints,
      start: Instant,
    ): Eval[OrderIntentConveyor] = {
      val orderStatesByIdEval = BasicOrderTrackingStateByIdEval(
        trades = InputEval(TradeHistoryInput(market, start)),
        observationEvents = InputEval(OrderSnapshotHistoryInput(market)).map(_.allObservationEvents),
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

  class SimulationOrderIntentConveyorFactory() extends OrderIntentConveyorFactory {

    override def apply(
      market: Market,
      orderConstraints: OrderConstraints,
      startTime: Instant,
    ): Eval[OrderIntentConveyor] = {
      OrderIntentConveyor(
        market = market,
        orderConstraintsEval = Constant(orderConstraints),
        orderIntentSyncer = Constant(SimpleOrderIntentSyncer(OrderMatcher.ExactMatcher)),
        openOrdersEval = InputEval(SimulatedOpenOrdersInput(market)),
        isInSyncEval = Constant(true),
        hasOpenRequestsEval = Constant(false),
        nextMessageIdsEval = NextRequestIdsEval(Constant(BotId("")), InputEval(BotOutputHistory)),
      )
    }

  }

  class DummyOrderIntentConveyorFactory() extends OrderIntentConveyorFactory {
    private val dummyConveyor = new OrderIntentConveyor {
      override def apply(v1: Seq[OperationIntent.OrderIntent]): Iterable[BotOutput] = Seq()
    }

    override def apply(v1: Market, v2: OrderConstraints, v3: Instant): Eval[OrderIntentConveyor] =
      Constant(dummyConveyor)
  }

  def singleMarketStrategyBotFactory(
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    orderConstraints: OrderConstraints,
    strategy: SingleMarketStrategy,
    market: Market,
    orderIntentConveyorFactory: OrderIntentConveyorFactory,
  )(
    implicit executionContext: ExecutionContext,
  ): BotFactory[SingleMarketStrategyBot] = new BotFactory[SingleMarketStrategyBot] {

    def makeBot(
      operationPeriod: TimePeriod,
      totalValue: BigDecimal,
    ): Future[SingleMarketStrategyBot] =
      for {
        p <- getInitialPrice(market, strategy.candleLength, operationPeriod.start)
      } yield {
        val runConfiguration = SingleMarketStrategyBotRunConfiguration(
          market = market,
          operationPeriod = operationPeriod,
          initialPrice = p,
          initialResources = strategy.initialResources(
            totalQuoteValue = totalValue,
            initialPrice = p,
          ),
        )
        SingleMarketStrategyBot(
          strategy = strategy,
          runConfiguration = runConfiguration,
          orderIntentConveyorEval = orderIntentConveyorFactory.apply(market, orderConstraints, operationPeriod.start),
        )
      }

    private def getInitialPrice(market: Market, candleLength: Duration, startTime: Instant): Future[BigDecimal] = {
      for {
        loader <- candleHistoryLoaderProvider.getHistoryLoader(market, candleLength)
        history <- loader.load(
          start = startTime.minusSeconds(60 * 60 * 12),
          end = startTime,
        )
      } yield history.lastPrice.get
    }

  }

  def multiMarketStrategyBotFactory(
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    orderConstraints: OrderConstraints,
    strategy: MultiMarketStrategy,
    markets: Seq[Market],
    orderIntentConveyorFactory: OrderIntentConveyorFactory,
  )(
    implicit executionContext: ExecutionContext,
  ): BotFactory[MultiMarketStrategyBot] = new BotFactory[MultiMarketStrategyBot] {

    def makeBot(
      operationPeriod: TimePeriod,
      totalValue: BigDecimal,
    ): Future[MultiMarketStrategyBot] = {

      val initialPricesByMarketFuture = Future.sequence(
        markets.map { market =>
          getInitialPrice(market, strategy.candleLength, operationPeriod.start).map(p => market -> p)
        }
      ).map(_.toMap)

      for {
        initialPricesByMarket <- initialPricesByMarketFuture
      } yield {
        val runConfiguration = MultiMarketStrategyBotRunConfiguration(
          operationPeriod = operationPeriod,
          initialValue = totalValue,
          initialPricesByMarket = initialPricesByMarket,
          initialBalances = strategy.calculateInitialBalances(
            totalQuoteValue = totalValue,
            initialPrices = initialPricesByMarket,
          ),
        )
        val orderIntentConveyorsByMarketEval = Eval.sequence(
          markets.map { market =>
            orderIntentConveyorFactory.apply(
              market,
              orderConstraints,
              operationPeriod.start,
            ).map(market -> _)
          }
        ).map(_.toMap)
        MultiMarketStrategyBot(
          strategy = strategy,
          runConfiguration = runConfiguration,
          orderIntentConveyorsByMarketEval = orderIntentConveyorsByMarketEval,
        )
      }
    }

    private def getInitialPrice(market: Market, candleLength: Duration, startTime: Instant): Future[BigDecimal] =
      for {
        loader <- candleHistoryLoaderProvider.getHistoryLoader(market, candleLength)
        history <- loader.load(
          start = startTime.minusSeconds(60 * 60 * 12),
          end = startTime,
        )
      } yield history.lastPrice.get

  }

}
