package io.liquirium.connect.deribit

case class DeribitTradesResponse(trades: Iterable[DeribitTrade], hasMore: Boolean)
