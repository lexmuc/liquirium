package io.liquirium.bot

import io.liquirium.util.TimePeriod

import scala.concurrent.Future

trait BotFactory[B <: EvalBot] {

  def makeBot(
    operationPeriod: TimePeriod,
    totalValue: BigDecimal,
  ): Future[B]

}
