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
  ): VisualizationLogger = InitState(VisualizationConfig(
    latestCandle = latestCandle,
    candleStartEvals = candleStartEvals,
    candleEndEvals = candleEndEvals,
  ))

  private case class InitState(config: VisualizationConfig) extends VisualizationLogger {
    private val firstStateEval = for {
      optCandle <- config.latestCandle
      startValuesMap <- config.candleStartMapEval
    } yield MainImpl(
      config = config,
      lastCandle = optCandle,
      visualizationUpdates = Vector(),
      candleStartValues = startValuesMap,
    )

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) =
      context.evaluate(firstStateEval)

    override def visualizationUpdates: Iterable[VisualizationUpdate] = Vector()

  }

  private case class MainImpl(
    config: VisualizationConfig,
    lastCandle: Option[Candle],
    visualizationUpdates: Vector[VisualizationUpdate] = Vector(),
    candleStartValues: Map[String, BigDecimal],
  ) extends VisualizationLogger {

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) = {
      def candleChangeEval(optCandle: Option[Candle]) = for {
        endAndStartValues <- config.endStartTupleEval
      } yield {
        val (endValues, startValues) = endAndStartValues
        copy(
          lastCandle = optCandle,
          candleStartValues = startValues,
        ).append(VisualizationUpdate(optCandle.get, candleStartValues ++ endValues))
      }

      val tempEval = config.latestCandle.flatMap { optCandle =>
        if (optCandle != lastCandle) candleChangeEval(optCandle) else Eval.unit(this)
      }

      context.evaluate(tempEval)

    }

    private def append(update: VisualizationUpdate) = copy(visualizationUpdates = visualizationUpdates :+ update)

  }

}
