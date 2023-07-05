package io.liquirium.bot

import io.liquirium.core.Market

import java.time.Instant
import scala.concurrent.Future

trait SingleMarketBotFactory {

  def makeBot(
    strategy: SingleMarketStrategy,
    market: Market,
    startTime: Instant,
    endTime: Option[Instant],
    totalValue: BigDecimal,
  ): Future[SingleMarketStrategyBot]

}
