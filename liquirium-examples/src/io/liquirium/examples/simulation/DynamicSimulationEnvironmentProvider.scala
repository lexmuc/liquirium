package io.liquirium.examples.simulation

import io.liquirium.bot.simulation._
import java.time.Instant

class DynamicSimulationEnvironmentProvider(
  simulationMarketplaceFactory: SimulationMarketplaceFactory,
  inputUpdateStreamProvider: SingleInputUpdateStreamProvider,
) extends SimulationEnvironmentProvider {

  def apply(
    simulationStart: Instant,
    simulationEnd: Instant,
  ): DynamicInputSimulationEnvironment =
    DynamicInputSimulationEnvironment(
      inputUpdateStream = SimulationInputUpdateStream(
        start = simulationStart,
        end = simulationEnd,
        singleInputStreamProvider = inputUpdateStreamProvider,
      ),
      marketplaces = SimulationMarketplaces(Seq(), m => simulationMarketplaceFactory(m, simulationStart)),
    )

}
