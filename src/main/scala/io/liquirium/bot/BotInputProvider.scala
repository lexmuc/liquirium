package io.liquirium.bot

import akka.NotUsed
import akka.stream.scaladsl.Source

trait BotInputProvider {

  def getInputUpdateStream[T](input: BotInput[T]): Option[Source[T, NotUsed]]

}
