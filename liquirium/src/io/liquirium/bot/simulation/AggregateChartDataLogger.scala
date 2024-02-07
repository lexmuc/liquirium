package io.liquirium.bot.simulation

import io.liquirium.core.Market
import io.liquirium.eval.{EvalResult, UpdatableContext}

case class AggregateChartDataLogger(
  marketsWithMarketLoggers: Seq[(Market, ChartDataLogger)],
) extends SimulationLogger[AggregateChartDataLogger] {

  override def log(context: UpdatableContext): (EvalResult[AggregateChartDataLogger], UpdatableContext) = {
    val (internalEvalResult, finalContext) =
      AggregateSimulationLogger(marketsWithMarketLoggers.map(_._2)).log(context)

    val evalResult = internalEvalResult.map { internalLogger =>
      AggregateChartDataLogger(
        marketsWithMarketLoggers.map(_._1).zip(internalLogger.subLoggers)
      )
    }
    (evalResult, finalContext)
  }

}
