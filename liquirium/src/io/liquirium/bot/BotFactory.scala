package io.liquirium.bot

import java.time.Instant
import scala.concurrent.Future

trait BotFactory[B <: EvalBot] {

  def makeBot(
    startTime: Instant,
    endTimeOption: Option[Instant],
    totalValue: BigDecimal,
  ): Future[B]

}
