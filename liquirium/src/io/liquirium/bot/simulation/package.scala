package io.liquirium.bot

import io.liquirium.bot.BotInput._
import io.liquirium.core._
import io.liquirium.eval._
import io.liquirium.util.store.{CandleHistoryLoaderProvider, TradeHistoryLoaderProvider}

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import io.liquirium.connect.binance.{exchangeId => binanceExchangeId}

package object simulation {

  def chartDataJsonSerializer = new ChartDataJsonSerializer(new ChartDataSeriesConfigJsonSerializer())

  def aggregateChartDataLogger(
    markets: Seq[Market],
    startTime: Instant,
    loggerFactory: ChartDataLoggerFactory,
  ): AggregateSimulationLogger[ChartDataLogger] = {
    AggregateSimulationLogger(markets.map (m => loggerFactory.getLogger(m, startTime)))
  }

  def simulationSingleInputUpdateStreamProvider(
    candleHistoryLoaderProvider: CandleHistoryLoaderProvider,
    tradeHistoryLoaderProvider: TradeHistoryLoaderProvider,
  )(
    implicit ec: ExecutionContext,
  ): SingleInputUpdateStreamProvider = new SingleInputUpdateStreamProvider {

    override def getInputStream(input: Input[_], start: Instant, end: Instant): Option[Stream[(Instant, Any)]] =
      input match {

        case ti: TimeInput => Some(TimedInputUpdateStream.forTimeInput(ti, start, end = end))

        case chi: CandleHistoryInput =>
          val candlesFuture = for {
            loader <- candleHistoryLoaderProvider.getHistoryLoader(chi.market, chi.candleLength)
            history <- loader.load(
              start = chi.start,
              end = end,
            )
          } yield history
          val candles = Await.result(candlesFuture, 3000.seconds)
          Some(TimedInputUpdateStream.forCandleHistory(chi, candles).dropWhile(_._1.isBefore(start)))

        case thi: TradeHistoryInput =>
          val tradesFuture = for {
            loader <- tradeHistoryLoaderProvider.getHistoryLoader(thi.market)
            history <- loader.loadHistory(
              start = thi.start,
              maybeEnd = Some(end),
            )
          } yield history
          val trades = Await.result(tradesFuture, 300.seconds)
          Some(TimedInputUpdateStream.forTradeHistory(thi, trades).dropWhile(_._1.isBefore(start)))

        case _ => None

      }

  }

  def simulationEnvironmentProvider(
    marketplaceFactory: SimulationMarketplaceFactory,
    inputUpdateStreamProvider: SingleInputUpdateStreamProvider,
  ): SimulationEnvironmentProvider = new SimulationEnvironmentProvider {

    def apply(
      simulationStart: Instant,
      simulationEnd: Instant,
    ): DynamicInputSimulationEnvironment =
      DynamicInputSimulationEnvironment(
        inputUpdateStream = SimulationInputUpdateStream(
          start = simulationStart,
          end = simulationEnd,
          singleInputStreamProvider = inputUpdateStreamProvider,
        ),
        marketplaces = SimulationMarketplaces(Seq(), m => marketplaceFactory(m, simulationStart)),
      )

  }

  def simulationMarketplaceFactory(
    candleLength: Duration,
  ): SimulationMarketplaceFactory = new SimulationMarketplaceFactory {

    def apply(
      market: Market,
      simulationStart: Instant,
    ): SimulationMarketplace =
      CandleSimulatorMarketplace(
        market = market,
        candlesEval = InputEval(CandleHistoryInput(market, candleLength, simulationStart)),
        simulator = getCandleSimulator(market),
        orderIds = orderIds(market),
        simulationStartTime = simulationStart,
      )

    private def getCandleSimulator(m: Market): CandleSimulator =
      m.exchangeId match {
        case `binanceExchangeId` => ScaledCandleSimulator(
          IncomeFractionFeeLevel(BigDecimal("0.00075")),
          volumeReduction = 1.0,
          tradeIds(m),
        )
        case eid => throw new RuntimeException(s"unknown exchange id: $eid")
      }

    private def orderIds(m: Market): Stream[String] =
      Stream.from(1).map(
        n => m.exchangeId.value + "-" + m.tradingPair.base + "-" + m.tradingPair.quote + "-" + n.toString
      )

    private def tradeIds(m: Market): Stream[StringTradeId] =
      Stream.from(1).map(
        n => StringTradeId(m.exchangeId.value + "-" + m.tradingPair.base + "-" + m.tradingPair.quote + "-" + n.toString)
      )

  }

}
