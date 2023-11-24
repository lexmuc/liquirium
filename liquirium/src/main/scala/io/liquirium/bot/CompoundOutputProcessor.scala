package io.liquirium.bot

case class CompoundOutputProcessor(subProcessors: Seq[BotOutputProcessor]) extends BotOutputProcessor {

  override def processOutput(output: BotOutput): Boolean =
    subProcessors.toStream.collectFirst { case sp if sp.processOutput(output) => sp }.isDefined

}
