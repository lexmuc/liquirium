package io.liquirium.bot.simulation

import io.liquirium.bot.EvalBot
import io.liquirium.core.Market

import java.time.Duration

trait BotWithSimulationInfo extends EvalBot {

  def basicCandleLength: Duration

  def metrics: Map[String, VisualizationMetric]

  def markets: Seq[Market]

}
