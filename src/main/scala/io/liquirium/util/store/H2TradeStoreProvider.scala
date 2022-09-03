package io.liquirium.util.store
import io.liquirium.core.Market

class H2TradeStoreProvider(
  getStoreForMarketStringAndMarket: (String, Market) => TradeStore,
) extends TradeStoreProvider {

  override def getStore(market: Market): TradeStore = {
    val marketString = s"${market.exchangeId.value}-${market.tradingPair.base}-${market.tradingPair.quote}"
    getStoreForMarketStringAndMarket(s"$marketString", market)
  }

}
