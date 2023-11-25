package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.VisualizationLogger.VisualizationConfig
import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.eval.{Eval, EvalResult, UpdatableContext}


trait VisualizationLogger extends SimulationLogger[VisualizationLogger] {

  override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext)

  def visualizationUpdates: Iterable[VisualizationUpdate]

  protected def config: VisualizationConfig

}

object VisualizationLogger {

  protected case class VisualizationConfig(
    candlesEval: Eval[CandleHistorySegment],
    candleStartEvals: Map[String, Eval[BigDecimal]],
    candleEndEvals: Map[String, Eval[BigDecimal]],
  ) {

    private def toMapEval(mapOfEvals: Map[String, Eval[BigDecimal]]) = Eval.sequence(
      mapOfEvals.map { case (k, v) => v.map(x => (k, x)) }
    ).map(_.toMap)

    val candleStartMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleStartEvals)
    val candleEndMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleEndEvals)

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
  ): VisualizationLogger =
    Impl(
      config = VisualizationConfig(
        candlesEval = candlesEval,
        candleStartEvals = candleStartEvals,
        candleEndEvals = candleEndEvals,
      ),
      lastCandles = None,
      visualizationUpdates = Vector(),
      candleStartValues = Map[String, BigDecimal](),
    )

  private case class Impl(
    config: VisualizationConfig,
    lastCandles: Option[CandleHistorySegment],
    visualizationUpdates: Vector[VisualizationUpdate],
    candleStartValues: Map[String, BigDecimal],
  ) extends VisualizationLogger {

    private def logCandle(
      candle: Candle,
      startValues: Map[String, BigDecimal],
      endValues: Map[String, BigDecimal],
    ) = copy(
      candleStartValues = startValues,
      visualizationUpdates = visualizationUpdates :+ VisualizationUpdate(candle, candleStartValues ++ endValues)
    )

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) = {
      context.evaluate(config.candlesAndValuesEval) match {
        case (evalResult, newContext) =>
          val mappedResult = evalResult.map {
            case (candles, startValues, endValues) =>

              if (lastCandles.isDefined) {
                if (candles != lastCandles.get) {
                  val newCandles = candles.incrementsAfter(lastCandles.get)
                  if (newCandles.size != 1) {
                    throw new RuntimeException(
                      "VisualizationLogger expected exactly 1 new candle but was " + newCandles.size
                    )
                  }
                  logCandle(newCandles.head, startValues, endValues).copy(lastCandles = Some(candles))
                } else this
              }

              else {
                if (candles.nonEmpty) {
                  throw new RuntimeException(
                    s"VisualizationLogger was called for the first time but there are already ${candles.size} candle(s)"
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
  }

}
