package io.liquirium.bot.simulation

import io.liquirium.bot.EvalBot

import java.time.Duration

trait BotWithSimulationInfo extends EvalBot {

  def basicCandleLength: Duration

  def metrics: Map[String, VisualizationMetric]

}
