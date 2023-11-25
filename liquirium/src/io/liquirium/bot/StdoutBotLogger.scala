package io.liquirium.bot

class StdoutBotLogger extends BotOutputProcessor {

  override def processOutput(output: BotOutput): Boolean =
    output match {
      case ble: BotLogEntry =>
        println(ble)
        true
      case _ =>
        false
    }

}
