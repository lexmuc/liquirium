package io.liquirium.bot.helpers

import io.liquirium.bot.{BotLogEntry, BotOutput}

object BotOutputHelpers {

  case class TestLogOutput(n: Int) extends BotLogEntry

  def output(n: Int): BotOutput = TestLogOutput(n)

  def logOutput(n: Int): BotOutput = TestLogOutput(n)

}
