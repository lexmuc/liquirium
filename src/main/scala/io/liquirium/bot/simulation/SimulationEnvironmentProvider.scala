package io.liquirium.bot.simulation

import java.time.Instant

trait SimulationEnvironmentProvider {

  def apply(simulationStart: Instant, simulationEnd: Instant): SimulationEnvironment

}

