package io.liquirium.util.store
import io.liquirium.core.Market

class H2TradeStoreProvider(
  getStoreForMarketStringAndMarket: (String, Market) => TradeStore,
) extends TradeStoreProvider {

  override def getStore(market: Market): TradeStore = {
    // # duplicate logic. Maybe we should not use the trade store provider at all (only cache) or refactor it to
    // use the connection or a connection provider instead
    val marketString = s"${market.exchangeId.value}-${market.tradingPair.base}-${market.tradingPair.quote}"
    // #TODO refactor. id/market string is not used
    getStoreForMarketStringAndMarket(s"$marketString", market)
  }

}
