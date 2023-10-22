package io.liquirium.bot

import io.liquirium.bot.simulation.BotWithSimulationInfo

import java.time.Instant
import scala.concurrent.Future

trait BotFactory {

  def makeBot(
    startTime: Instant,
    endTimeOption: Option[Instant],
    totalValue: BigDecimal,
  ): Future[BotWithSimulationInfo]

}
