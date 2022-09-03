package io.liquirium.bot

import akka.NotUsed
import akka.stream.scaladsl.Source

case class CompoundInputProvider(subProviders: Seq[BotInputProvider]) extends BotInputProvider {

  override def getInputUpdateStream[T](input: BotInput[T]): Option[Source[T, NotUsed]] =
    subProviders.toStream.map(_.getInputUpdateStream(input)).collectFirst { case Some(s) => s }

}
