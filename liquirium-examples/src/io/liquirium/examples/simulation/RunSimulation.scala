package io.liquirium.examples.simulation

import io.liquirium.util.akka.DefaultConcurrencyContext
import io.liquirium.bot.BotInput.CandleHistoryInput
import io.liquirium.bot.SingleMarketStrategyBotUtils
import io.liquirium.bot.simulation._
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core._
import io.liquirium.eval.{Eval, InputEval}
import io.liquirium.examples.dca.DollarCostAverageStrategy
import io.liquirium.util.{ApiCredentials, NumberPrecision}
import io.liquirium.util.store.TradeHistoryLoaderProvider

import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object RunSimulation extends App {

  private def run(): Unit = {

    implicit val ec: ExecutionContext = DefaultConcurrencyContext.executionContext

    val runDuration = Duration.ofDays(100)
    val strategy = DollarCostAverageStrategy(runDuration)
    val totalValue = BigDecimal(10000)
    val market = Market(io.liquirium.connect.binance.exchangeId, TradingPair("BTC", "USDT"))

    val simulationStart = Instant.parse("2023-07-01T00:00:00.000Z")
    val simulationEnd = simulationStart plus runDuration

    val binanceExchangeConnectorFuture = io.liquirium.connect.binance.getConnector(
      concurrencyContext = DefaultConcurrencyContext,
      // for the simulation we only need to load candles which we can load without credentials
      credentials = ApiCredentials.apply("", ""),
    )

    val exchangeConnectorProvider = new(ExchangeId => Future[ExchangeConnector]) {
      override def apply(exchangeId: ExchangeId): Future[ExchangeConnector] = exchangeId match {
        case io.liquirium.connect.binance.exchangeId => binanceExchangeConnectorFuture
        case _ => throw new RuntimeException("Exchange not supported: " + exchangeId)
      }
    }

    val candleHistoryLoaderProvider = io.liquirium.util.store.getCachingCandleHistoryLoaderProvider(
      exchangeConnectorProvider,
    )

    val botFactory = io.liquirium.bot.singleMarketStrategyBotFactoryForSimulation(
      candleHistoryLoaderProvider = candleHistoryLoaderProvider,
      orderConstraints = OrderConstraints(
        pricePrecision = NumberPrecision.significantDigits(5),
        quantityPrecision = NumberPrecision.significantDigits(5),
      ),
      strategy = strategy,
      market = market,
      metricsFactory = bot => SingleMarketStrategyBotUtils.getDataSeriesConfigs(bot),
    )

    val botFuture = botFactory.makeBot(
      startTime = simulationStart,
      endTimeOption = Some(simulationEnd),
      totalValue = totalValue,
    )
    val bot = Await.result(botFuture, 1.minute)

    def getCandlesEvalForLogger(market: Market): Eval[CandleHistorySegment] =
      CandleAggregationFold.aggregate(
        InputEval(CandleHistoryInput(market, bot.basicCandleLength, simulationStart)),
        Duration.ofHours(6),
      )

    def makeSingleMarketChartDataLogger(market: Market): ChartDataLogger = {
      val candlesEval = getCandlesEvalForLogger(market)
      ChartDataLogger(
        candlesEval = candlesEval,
        dataSeriesConfigsWithEvals = bot.chartDataSeriesConfigs.map(dsc => {
          val e = dsc.metric.getEval(market, simulationStart, candlesEval)
          (dsc, e)
        }),
      )
    }

    val dummyTradeHistoryLoaderProvider = new TradeHistoryLoaderProvider {
      override def getHistoryLoader(market: Market): Future[TradeHistoryLoader] =
        throw new RuntimeException("Trade history loader should not be required in simulation.")
    }

    val singleInputUpdateStreamProvider = simulationSingleInputUpdateStreamProvider(
      candleHistoryLoaderProvider = candleHistoryLoaderProvider,
      tradeHistoryLoaderProvider = dummyTradeHistoryLoaderProvider,
    )

    val simulationEnvironmentProvider = new DynamicSimulationEnvironmentProvider(
      inputUpdateStreamProvider = singleInputUpdateStreamProvider,
      simulationMarketplaceFactory = simulationMarketplaceFactory(bot.basicCandleLength),
    )

    val logger = AggregateChartDataLogger(
      bot.markets.map(m => m -> makeSingleMarketChartDataLogger(m)),
    )

    val botSimulator = new EvalBotSimulatorFactory(
      simulationEnvironmentProvider = simulationEnvironmentProvider,
      withTradeHistoryInput = true,
    ).getSimulator(
      simulationStart = simulationStart,
      simulationEnd = simulationEnd,
      bot.eval,
      logger,
    )

    val stopwatchStart = Instant.now()
    val finalLogger = botSimulator.run()
    val stopwatchEnd = Instant.now()

    println("simulation took " + (stopwatchEnd.getEpochSecond - stopwatchStart.getEpochSecond) + " seconds.")

    Files.write(
      Paths.get("visualization/data.json"),
      chartDataJsonSerializer.serialize(finalLogger.marketsWithMarketLoggers).toString.getBytes
    )

    println("simulation finished")
  }

  // if we don't terminate the actor system afterwards the script will never complete
  try {
    run()
  }
  finally {
    DefaultConcurrencyContext.actorSystem.terminate()
  }

}
