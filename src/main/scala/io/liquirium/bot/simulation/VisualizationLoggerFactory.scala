package io.liquirium.bot.simulation

import io.liquirium.core.Market

import java.time.Instant

trait VisualizationLoggerFactory {

  def getLogger(market: Market, startTime: Instant): VisualizationLogger

}
