package io.liquirium.bot.simulation

import io.liquirium.core.Market

import java.time.Instant

trait SimulationMarketplaceFactory {

  def apply(market: Market, simulationStart: Instant): SimulationMarketplace

}
