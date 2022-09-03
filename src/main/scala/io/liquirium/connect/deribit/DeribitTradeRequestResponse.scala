package io.liquirium.connect.deribit

trait DeribitTradeRequestResponse

case class DeribitOrderRequestResponse(order: DeribitOrder, trades: Seq[DeribitTrade])
  extends DeribitTradeRequestResponse

case class DeribitCancelRequestResponse(order: DeribitOrder) extends DeribitTradeRequestResponse
