package io.liquirium.bot

trait BotOutputProcessor {

  def processOutput(output: BotOutput): Boolean

}
