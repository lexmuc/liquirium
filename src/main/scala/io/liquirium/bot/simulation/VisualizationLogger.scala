package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.VisualizationLogger.VisualizationConfig
import io.liquirium.core.Candle
import io.liquirium.eval.{Eval, EvalResult, InputRequest, UpdatableContext, Value}


trait VisualizationLogger extends SimulationLogger[VisualizationLogger] {

  override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext)

  def visualizationUpdates: Iterable[VisualizationUpdate]

  protected def startNextCandle(
    lastCandle: Option[Candle],
    candleStartValues: Map[String, BigDecimal],
  ): VisualizationLogger

  protected def config: VisualizationConfig

  protected def tryEvaluation[E](context: UpdatableContext, eval: Eval[E])(
    f: (UpdatableContext, E) => (EvalResult[VisualizationLogger], UpdatableContext)
  ): (EvalResult[VisualizationLogger], UpdatableContext) = {
    val (evalResult, newContext) = context.evaluate(eval)
    evalResult match {
      case ir: InputRequest => (ir, newContext)
      case Value(v) => f(newContext, v)
    }
  }

}

object VisualizationLogger {

  private case class VisualizationConfig(
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
    latestCandle =  latestCandle,
    candleStartEvals = candleStartEvals,
    candleEndEvals = candleEndEvals,
  ))

  private case class InitState(config: VisualizationConfig) extends VisualizationLogger {

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) =
      tryEvaluation(context, config.latestCandle) { (afterCandleContext, optCandle) =>
        tryEvaluation(afterCandleContext, config.candleStartMapEval) { (finalContext, startValuesMap) =>
          val newLogger = MainImpl(
            config = config,
            lastCandle = optCandle,
            visualizationUpdates = Vector(),
            candleStartValues = startValuesMap,
          )
          (Value(newLogger), finalContext)
        }
      }

    override def visualizationUpdates: Iterable[VisualizationUpdate] = Vector()

    override def startNextCandle(lastCandle: Option[Candle], candleStartValues: Map[String, BigDecimal]): MainImpl =
      MainImpl(
        config = config,
        lastCandle = lastCandle,
        candleStartValues = candleStartValues,
      )

  }

  private case class MainImpl(
    config: VisualizationConfig,
    lastCandle: Option[Candle],
    visualizationUpdates: Vector[VisualizationUpdate] = Vector(),
    candleStartValues: Map[String, BigDecimal],
  ) extends VisualizationLogger {

    override def log(context: UpdatableContext): (EvalResult[VisualizationLogger], UpdatableContext) =
      tryEvaluation(context, config.latestCandle) { (afterCandleContext, optCandle) =>
        if (optCandle != lastCandle) {
          tryEvaluation(afterCandleContext, config.endStartTupleEval) {
            case (finalContext, (endValues, startValues)) =>
              val newLogger = startNextCandle(
                lastCandle = optCandle,
                candleStartValues = startValues,
              ).append(VisualizationUpdate(optCandle.get, candleStartValues ++ endValues))
              (Value(newLogger), finalContext)
          }
        }
        else {
          (Value(this), afterCandleContext)
        }
      }

    override def startNextCandle(lastCandle: Option[Candle], candleStartValues: Map[String, BigDecimal]): MainImpl =
      copy(
        lastCandle = lastCandle,
        candleStartValues = candleStartValues,
      )

    private def append(update: VisualizationUpdate) = copy(visualizationUpdates = visualizationUpdates :+ update)

  }

}
