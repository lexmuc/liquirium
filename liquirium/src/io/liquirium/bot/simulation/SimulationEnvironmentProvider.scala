package io.liquirium.bot.simulation


trait SimulationEnvironmentProvider {

  def apply(simulationPeriod: SimulationPeriod): SimulationEnvironment

}

