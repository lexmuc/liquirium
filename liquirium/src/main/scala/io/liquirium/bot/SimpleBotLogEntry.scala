package io.liquirium.bot

case class SimpleBotLogEntry(message: String) extends BotLogEntry {

  override def toString: String = message

}
