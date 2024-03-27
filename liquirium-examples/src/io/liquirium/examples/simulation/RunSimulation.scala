package io.liquirium.examples.simulation

import io.liquirium.bot.BotInput._
import io.liquirium.bot.{SimpleBotEvaluator, SingleMarketStrategy}
import io.liquirium.bot.simulation._
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core._
import io.liquirium.eval.InputEval
import io.liquirium.examples.dca.DollarCostAverageStrategy
import io.liquirium.util.{NumberPrecision, TimePeriod}
import io.liquirium.util.akka.DefaultConcurrencyContext
import io.liquirium.util.store.{CandleHistoryLoaderProvider, TradeHistoryLoaderProvider}

import java.awt.Desktop
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object RunSimulation extends App {

  private def run(): Unit = {
    val runDuration = Duration.ofDays(50)
    val strategy = DollarCostAverageStrategy(runDuration, candleLength = Duration.ofHours(1))
    val totalValue = BigDecimal(10000)
    val market = Market(io.liquirium.connect.binance.exchangeId, TradingPair("BTC", "USDT"))
    val getSimulationInfo = bot => DollarCostAverageStrategy.getSimulationInfo(bot)
    val simulationStart = Instant.parse("2024-01-01T00:00:00.000Z")
    val simulationPeriod = TimePeriod(
      start = simulationStart,
      end = simulationStart plus runDuration,
    )

    // We only read chart data from the exchange, so we don't need to authenticate
    val connector = Await.result(io.liquirium.connect.binance.getConnector(), 10.seconds)

    val candleHistoryLoaderProvider = getCandleHistoryLoaderProvider(connector)

    val bot = getBot(
      simulationPeriod = simulationPeriod,
      candleHistoryLoaderProvider = candleHistoryLoaderProvider,
      strategy = strategy,
      market = market,
      totalValue = totalValue,
    )

    val simulationEnvironment = getSimulationEnvironment(
      market = market,
      simulationPeriod = simulationPeriod,
      candleLength = strategy.candleLength,
      candleHistoryLoaderProvider = candleHistoryLoaderProvider,
    )

    val botSimulator = EvalBotSimulator(
      context = initialContextForSimulation(simulationStart),
      evaluator = SimpleBotEvaluator(bot.eval),
      environment = simulationEnvironment,
      logger = getLogger(getSimulationInfo(bot), simulationStart),
    )

    val stopwatchStart = Instant.now()
    val finalLogger = botSimulator.run()
    val stopwatchEnd = Instant.now()
    println("simulation took " + (stopwatchEnd.getEpochSecond - stopwatchStart.getEpochSecond) + " seconds.")

    val outputFilePath = Paths.get("liquirium-examples/charts/last-simulation.html").toAbsolutePath
    Files.createDirectories(outputFilePath.getParent)
    writeChart(outputFilePath, chartDataJsonSerializer.serialize(finalLogger.marketsWithMarketLoggers))
    Desktop.getDesktop.open(outputFilePath.toFile)

    println("simulation finished")
  }

  private def getBot(
    simulationPeriod: TimePeriod,
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    strategy: SingleMarketStrategy,
    market: Market,
    totalValue: BigDecimal,
  ) = {
    val botFactory = io.liquirium.bot.singleMarketStrategyBotFactory(
      candleHistoryLoaderProvider = candleHistoryLoaderProvider,
      orderConstraints = OrderConstraints(
        pricePrecision = NumberPrecision.significantDigits(5),
        quantityPrecision = NumberPrecision.significantDigits(5),
      ),
      strategy = strategy,
      market = market,
    )(DefaultConcurrencyContext.executionContext)

    val botFuture = botFactory.makeBot(simulationPeriod, totalValue)
    Await.result(botFuture, 1.minute)
  }

  private def getSimulationEnvironment(
    market: Market,
    simulationPeriod: TimePeriod,
    candleLength: Duration,
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
  ) = {
    // The trades are part of the output of the simulation, so we don't provide a trade history loader.
    // However, when tracking the performance of a bot running in production, we would provide the actual trades here.
    val dummyTradeHistoryLoaderProvider = new TradeHistoryLoaderProvider {
      override def getHistoryLoader(market: Market): Future[TradeHistoryLoader] =
        throw new RuntimeException("Trade history loader should not be required in simulation.")
    }

    val inputUpdateStream = SimulationInputUpdateStream(
      period = simulationPeriod,
      singleInputStreamProvider = simulationSingleInputUpdateStreamProvider(
        candleHistoryLoaderProvider = candleHistoryLoaderProvider,
        tradeHistoryLoaderProvider = dummyTradeHistoryLoaderProvider,
      )(DefaultConcurrencyContext.executionContext),
    )

    val marketplaceFactory = simulationMarketplaceFactory(candleLength)
    DynamicInputSimulationEnvironment(
      inputUpdateStream = inputUpdateStream,
      marketplaces = SimulationMarketplaces(Seq(marketplaceFactory(market, simulationPeriod.start))),
    )
  }

  private def getLogger(simulationInfo: BotSimulationInfo, simulationStart: Instant): AggregateChartDataLogger = {

    def makeSingleMarketChartDataLogger(market: Market): ChartDataLogger = {
      val candlesEval =
        CandleAggregationFold.aggregate(
          InputEval(CandleHistoryInput(market, simulationInfo.basicCandleLength, simulationStart)),
          Duration.ofHours(6),
        )
      ChartDataLogger(
        candlesEval = candlesEval,
        dataSeriesConfigsWithEvals = simulationInfo.chartDataSeriesConfigs.map(dsc => {
          val e = dsc.metric.getEval(market, simulationStart, candlesEval)
          (dsc, e)
        }),
      )
    }

    AggregateChartDataLogger(
      simulationInfo.markets.map(m => m -> makeSingleMarketChartDataLogger(m)),
    )
  }

  private def getCandleHistoryLoaderProvider(connector: ExchangeConnector) = {
    val connectorExchangeId = connector.exchangeId
    val exchangeConnectorProvider = new(ExchangeId => Future[ExchangeConnector]) {
      override def apply(exchangeId: ExchangeId): Future[ExchangeConnector] = exchangeId match {
        case `connectorExchangeId` => Future { connector }(DefaultConcurrencyContext.executionContext)
        case _ => throw new RuntimeException("Exchange not supported: " + exchangeId)
      }
    }
    io.liquirium.util.store.getCachingCandleHistoryLoaderProvider(
      getConnector = exchangeConnectorProvider,
      cacheDirectory = Paths.get("liquirium-examples/cache").toAbsolutePath,
    )
  }

  // if we don't terminate the actor system afterwards the script will never complete
  try {
    run()
  }
  finally {
    DefaultConcurrencyContext.actorSystem.terminate()
  }

}
