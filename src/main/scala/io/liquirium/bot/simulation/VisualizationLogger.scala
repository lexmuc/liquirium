package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.VisualizationLogger.VisualizationConfig
import io.liquirium.core.Candle
import io.liquirium.eval.{Eval, EvalResult, UpdatableContext}


trait VisualizationLogger extends SimulationLogger[VisualizationLogger] {

  override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext)

  def visualizationUpdates: Iterable[VisualizationUpdate]

  protected def config: VisualizationConfig

}

object VisualizationLogger {

  protected case class VisualizationConfig(
    latestCandle: Eval[Option[Candle]],
    candleStartEvals: Map[String, Eval[BigDecimal]],
    candleEndEvals: Map[String, Eval[BigDecimal]],
  ) {

    private def toMapEval(mapOfEvals: Map[String, Eval[BigDecimal]]) = Eval.sequence(
      mapOfEvals.map { case (k, v) => v.map(x => (k, x)) }
    ).map(_.toMap)

    val candleStartMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleStartEvals)
    val candleEndMapEval: Eval[Map[String, BigDecimal]] = toMapEval(candleEndEvals)

    val candleAndValuesEval: Eval[(Option[Candle], Map[String, BigDecimal], Map[String, BigDecimal])] = for {
      optCandle <- latestCandle
      startValues <- candleStartMapEval
      endValues <- candleEndMapEval
    } yield {
      (optCandle, startValues, endValues)
    }

  }

  def apply(
    latestCandle: Eval[Option[Candle]],
    candleStartEvals: Map[String, Eval[BigDecimal]],
    candleEndEvals: Map[String, Eval[BigDecimal]],
  ): VisualizationLogger =
    Impl(
      isInitialized = false,
      config = VisualizationConfig(
        latestCandle = latestCandle,
        candleStartEvals = candleStartEvals,
        candleEndEvals = candleEndEvals,
      ),
      lastCandle = None,
      visualizationUpdates = Vector(),
      candleStartValues = Map[String, BigDecimal](),
    )

  private case class Impl(
    isInitialized: Boolean,
    config: VisualizationConfig,
    lastCandle: Option[Candle],
    visualizationUpdates: Vector[VisualizationUpdate],
    candleStartValues: Map[String, BigDecimal],
  ) extends VisualizationLogger {

    private def logCandle(
      candle: Candle,
      startValues: Map[String, BigDecimal],
      endValues: Map[String, BigDecimal],
    ) = copy(
      lastCandle = Some(candle),
      candleStartValues = startValues,
      visualizationUpdates = visualizationUpdates :+ VisualizationUpdate(candle, candleStartValues ++ endValues)
    )

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) =
      if (isInitialized) {
        context.evaluate(config.candleAndValuesEval) match {
          case (evalResult, newContext) =>
            val mappedResult = evalResult.map {
              case (optCandle, startValues, endValues) =>
                if (optCandle != lastCandle) logCandle(optCandle.get, startValues, endValues) else this
            }
            (mappedResult, newContext)
        }
      }
      else {
        val initEval = for {
          optCandle <- config.latestCandle
          startValuesMap <- config.candleStartMapEval
        } yield copy(
          isInitialized = true,
          lastCandle = optCandle,
          candleStartValues = startValuesMap,
        )
        context.evaluate(initEval)
      }

  }

}
