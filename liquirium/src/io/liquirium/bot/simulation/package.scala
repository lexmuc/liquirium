package io.liquirium.bot

import io.liquirium.bot.BotInput._
import io.liquirium.core._
import io.liquirium.eval._
import io.liquirium.util.store.{CandleHistoryLoaderProvider, TradeHistoryLoaderProvider}

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import io.liquirium.connect.binance.{exchangeId => binanceExchangeId}
import io.liquirium.core.orderTracking.{OpenOrdersHistory, OpenOrdersSnapshot}
import play.api.libs.json.JsValue

import java.nio.file.{Files, Path}
import scala.io.Source

package object simulation {

  def chartDataJsonSerializer = new ChartDataJsonSerializer(new ChartDataSeriesConfigJsonSerializer())

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

  /**
   * Provides a context that can be used to simulate a bot.
   *
   * It would be preferable to provide the initial values for the inputs in the classes where the updates are generated,
   * but this would require a change of the way the simulation works. So for the time being we use this workaround.
   */
  def initialContextForSimulation(
    simulationStart: Instant,
    withTradeHistoryInput: Boolean = true, // deactivate when tracking the performance of a bot running in production
    baseContext: UpdatableContext = ExplicitCacheContext(),
  ): UpdatableContext =
    ContextWithInputResolution(
      baseContext = baseContext,
      resolve = {
        case CompletedOperationRequestsInSession => Some(IncrementalSeq.empty)
        case OrderSnapshotHistoryInput(_) => Some(
          OpenOrdersHistory.start(OpenOrdersSnapshot(OrderSet.empty, Instant.ofEpochSecond(0)))
        )
        case TradeHistoryInput(_, start) if start == simulationStart && withTradeHistoryInput =>
          Some(TradeHistorySegment.empty(simulationStart))
        case SimulatedOpenOrdersInput(_) => Some(Set[Order]())
        case _ => None
      }
    )

  def writeChart(path: Path, dataJson: JsValue): Unit = {
    val template = Source.fromResource("chart-template.html").mkString
    val code = Source.fromResource("chart-code.js").mkString
    val chartHtml = template
      .replace("{{lightweightCharts}}", Source.fromResource("lightweight-charts.standalone.production.js").mkString)
      .replace("{{jquery}}", Source.fromResource("jquery-3.3.1.min.js").mkString)
      .replace("{{chartStyles}}", Source.fromResource("chart-styles.css").mkString)
      .replace("{{chartData}}", dataJson.toString)
      .replace("{{chartCode}}", code)
    Files.write(path, chartHtml.getBytes)
  }

}
