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

    val endStartTupleEval: Eval[(Map[String, BigDecimal], Map[String, BigDecimal])] =
      Eval.map2(candleEndMapEval, candleStartMapEval) { case (e, s) => (e, s) }

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

    private val regularEval = for {
      optCandle <- config.latestCandle
      endAndStartValues <- config.endStartTupleEval
    } yield {
      if (optCandle != lastCandle) logCandle(
        optCandle.get,
        endAndStartValues._2,
        endAndStartValues._1,
      ) else this
    }

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) =
      if (isInitialized) {
        context.evaluate(regularEval)
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
