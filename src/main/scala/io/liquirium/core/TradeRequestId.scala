package io.liquirium.core

sealed trait TradeRequestId

case class CompoundTradeRequestId(botId: BotId, requestIndex: Long) extends TradeRequestId {

  override def toString: String = botId.toString + "-" + requestIndex.toString

}
