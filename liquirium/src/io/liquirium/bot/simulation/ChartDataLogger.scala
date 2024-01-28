package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.ChartDataLogger.ChartConfig
import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.eval.{Eval, EvalResult, UpdatableContext}


trait ChartDataLogger extends SimulationLogger[ChartDataLogger] {

  override def log(context: UpdatableContext): (EvalResult[ChartDataLogger], UpdatableContext)

  def chartDataUpdates: Iterable[ChartDataUpdate]

  def dataSeriesConfigsByKey: Map[String, DataSeriesConfig]

  protected def config: ChartConfig

}

object ChartDataLogger {

  protected case class ChartConfig(
    candlesEval: Eval[CandleHistorySegment],
    candleStartEvals: Map[String, Eval[BigDecimal]],
    candleEndEvals: Map[String, Eval[BigDecimal]],
    dataSeriesConfigsByKey: Map[String, DataSeriesConfig],
  ) {

    private def toMapEval(mapOfEvals: Map[String, Eval[BigDecimal]]) = Eval.sequence(
      mapOfEvals.map { case (k, v) => v.map(x => (k, x)) }
    ).map(_.toMap)

    private val candleStartMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleStartEvals)
    private val candleEndMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleEndEvals)

    val candlesAndValuesEval: Eval[(CandleHistorySegment, Map[String, BigDecimal], Map[String, BigDecimal])] = for {
      candles <- candlesEval
      startValues <- candleStartMapEval
      endValues <- candleEndMapEval
    } yield (candles, startValues, endValues)

  }

  def apply(
    candlesEval: Eval[CandleHistorySegment],
    candleStartEvals: Map[String, Eval[BigDecimal]],
    candleEndEvals: Map[String, Eval[BigDecimal]],
    dataSeriesConfigsByKey: Map[String, DataSeriesConfig],
  ): ChartDataLogger =
    Impl(
      config = ChartConfig(
        candlesEval = candlesEval,
        candleStartEvals = candleStartEvals,
        candleEndEvals = candleEndEvals,
        dataSeriesConfigsByKey = dataSeriesConfigsByKey,
      ),
      lastCandles = None,
      chartDataUpdates = Vector(),
      candleStartValues = Map[String, BigDecimal](),
    )

  private case class Impl(
    config: ChartConfig,
    lastCandles: Option[CandleHistorySegment],
    chartDataUpdates: Vector[ChartDataUpdate],
    candleStartValues: Map[String, BigDecimal],
  ) extends ChartDataLogger {

    private def logCandle(
      candle: Candle,
      startValues: Map[String, BigDecimal],
      endValues: Map[String, BigDecimal],
    ) = copy(
      candleStartValues = startValues,
      chartDataUpdates = chartDataUpdates :+ ChartDataUpdate(candle, candleStartValues ++ endValues)
    )

    override def log(context: UpdatableContext): (EvalResult[ChartDataLogger], UpdatableContext) = {
      context.evaluate(config.candlesAndValuesEval) match {
        case (evalResult, newContext) =>
          val mappedResult = evalResult.map {
            case (candles, startValues, endValues) =>

              if (lastCandles.isDefined) {
                if (candles != lastCandles.get) {
                  val newCandles = candles.incrementsAfter(lastCandles.get)
                  if (newCandles.size != 1) {
                    throw new RuntimeException(
                      "ChartDataLogger expected exactly 1 new candle but was " + newCandles.size
                    )
                  }
                  logCandle(newCandles.head, startValues, endValues).copy(lastCandles = Some(candles))
                } else this
              }

              else {
                if (candles.nonEmpty) {
                  throw new RuntimeException(
                    s"ChartDataLogger was called for the first time but there are already ${candles.size} candle(s)"
                  )
                }
                copy(
                  lastCandles = Some(candles),
                  candleStartValues = startValues,
                )
              }

          }
          (mappedResult, newContext)
      }
    }

    override def dataSeriesConfigsByKey: Map[String, DataSeriesConfig] = config.dataSeriesConfigsByKey

  }

}
