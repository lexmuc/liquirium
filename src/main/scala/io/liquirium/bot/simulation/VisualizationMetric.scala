package io.liquirium.bot.simulation

import io.liquirium.core.Market
import io.liquirium.eval.Eval

import java.time.Instant

object VisualizationMetric {

  trait MetricType

  object MetricType {
    case object CandleStart extends MetricType
    case object CandleEnd extends MetricType
  }

}

trait VisualizationMetric {

  def metricType: VisualizationMetric.MetricType

  def getEval(market: Market, startTime: Instant): Eval[BigDecimal]

}
