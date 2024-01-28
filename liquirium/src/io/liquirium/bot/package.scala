package io.liquirium

import io.liquirium.bot.BotInput._
import io.liquirium.bot.simulation.{BotWithSimulationInfo, ChartMetric}
import io.liquirium.core.orderTracking._
import io.liquirium.core.{BotId, Market, OrderConstraints}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, InputEval}
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

  def singleMarketStrategyBotFactoryForSimulation(
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    orderConstraints: OrderConstraints,
    strategy: SingleMarketStrategy,
    market: Market,
    metricsFactory: SingleMarketStrategyBot => Map[String, ChartMetric],
  )(
    implicit executionContext: ExecutionContext,
  ): BotFactory = new BotFactory {

    def makeBot(
      startTime: Instant,
      endTimeOption: Option[Instant],
      totalValue: BigDecimal,
    ): Future[BotWithSimulationInfo] =
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
        val orderIntentConveyorFactory = new ProductionOrderIntentConveyorFactory(
          syncInterval = Duration.ofSeconds(150),
        )
        val coreBot = SingleMarketStrategyBot(
          strategy = strategy,
          runConfiguration = runConfiguration,
          orderIntentConveyorEval = orderIntentConveyorFactory.apply(
            market = market,
            orderConstraints = orderConstraints,
            start = startTime,
          ),
        )
        new BotWithSimulationInfo {
          override def basicCandleLength: Duration = coreBot.strategy.candleLength

          override def metrics: Map[String, ChartMetric] = metricsFactory.apply(coreBot)

          override def markets: Seq[Market] = Seq(market)

          override def eval: Eval[Iterable[BotOutput]] = coreBot.eval
        }
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

}
