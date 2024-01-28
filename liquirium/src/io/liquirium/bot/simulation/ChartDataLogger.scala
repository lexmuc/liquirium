package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.ChartDataLogger.ChartConfig
import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime
import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.eval.{Eval, EvalResult, UpdatableContext}


trait ChartDataLogger extends SimulationLogger[ChartDataLogger] {

  override def log(context: UpdatableContext): (EvalResult[ChartDataLogger], UpdatableContext)

  def chartDataUpdates: Iterable[ChartDataUpdate]

  def dataSeriesConfigs: Seq[ChartDataSeriesConfig]

  protected def config: ChartConfig

}

object ChartDataLogger {

  protected case class ChartConfig(
    candlesEval: Eval[CandleHistorySegment],
    dataSeriesConfigsWithEvals: Seq[(ChartDataSeriesConfig, Eval[BigDecimal])],
  ) {

    private def toMapEval(mapOfEvals: Map[Int, Eval[BigDecimal]]) = Eval.sequence(
      mapOfEvals.map { case (k, v) => v.map(x => (k, x)) }
    ).map(_.toMap)

    private val candleStartEvals: Map[Int, Eval[BigDecimal]] = dataSeriesConfigsWithEvals.zipWithIndex
      .filter(_._1._1.snapshotTime == SnapshotTime.CandleStart).map {
      case ((_, eval), index) =>
          index -> eval
    }.toMap

    private val candleEndEvals: Map[Int, Eval[BigDecimal]] = dataSeriesConfigsWithEvals.zipWithIndex
      .filter(_._1._1.snapshotTime == SnapshotTime.CandleEnd).map {
        case ((_, eval), index) =>
          index -> eval
      }.toMap

    private val candleStartMapEval: Eval[Map[Int, BigDecimal]] = toMapEval(candleStartEvals)
    private val candleEndMapEval: Eval[Map[Int, BigDecimal]] = toMapEval(candleEndEvals)

    val candlesAndValuesEval: Eval[(CandleHistorySegment, Map[Int, BigDecimal], Map[Int, BigDecimal])] = for {
      candles <- candlesEval
      startValues <- candleStartMapEval
      endValues <- candleEndMapEval
    } yield (candles, startValues, endValues)

  }

  def apply(
    candlesEval: Eval[CandleHistorySegment],
    dataSeriesConfigsWithEvals: Seq[(ChartDataSeriesConfig, Eval[BigDecimal])],
  ): ChartDataLogger =
    Impl(
      config = ChartConfig(
        candlesEval = candlesEval,
        dataSeriesConfigsWithEvals = dataSeriesConfigsWithEvals,
      ),
      lastCandles = None,
      chartDataUpdates = Vector(),
      candleStartValues = Map[Int, BigDecimal](),
    )

  private case class Impl(
    config: ChartConfig,
    lastCandles: Option[CandleHistorySegment],
    chartDataUpdates: Vector[ChartDataUpdate],
    candleStartValues: Map[Int, BigDecimal],
  ) extends ChartDataLogger {

    private def logCandle(
      candle: Candle,
      startValues: Map[Int, BigDecimal],
      endValues: Map[Int, BigDecimal],
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

    override def dataSeriesConfigs: Seq[ChartDataSeriesConfig] = config.dataSeriesConfigsWithEvals.map(_._1)

  }

}
