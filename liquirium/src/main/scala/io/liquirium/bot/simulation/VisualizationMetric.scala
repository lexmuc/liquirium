package io.liquirium.bot.simulation

import io.liquirium.core.Market
import io.liquirium.eval.Eval

import java.time.Instant

trait VisualizationMetric {

  def getEval(market: Market, startTime: Instant): Eval[BigDecimal]

}
