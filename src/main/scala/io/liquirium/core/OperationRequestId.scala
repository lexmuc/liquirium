package io.liquirium.core

sealed trait OperationRequestId

case class CompoundOperationRequestId(botId: BotId, requestIndex: Long) extends OperationRequestId {

  override def toString: String = botId.toString + "-" + requestIndex.toString

}
