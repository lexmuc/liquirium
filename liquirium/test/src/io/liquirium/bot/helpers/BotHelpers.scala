package io.liquirium.bot.helpers

import io.liquirium.bot.{BotLogEntry, BotOutput, SimpleBotLogEntry, OperationRequestMessage}


object BotHelpers {

  def botOutput(n: Int): BotOutput =
    if (n % 2 == 0) OperationRequestMessage(OperationRequestHelpers.id(n), OperationRequestHelpers.operationRequest(n))
    else SimpleBotLogEntry(n.toString)

  def botLogEntry(n: Int): BotLogEntry = SimpleBotLogEntry(n.toString)

}
