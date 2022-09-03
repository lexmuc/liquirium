package io.liquirium.bot

trait BotLogger {

  def log(entry: BotLogEntry): Unit

}
