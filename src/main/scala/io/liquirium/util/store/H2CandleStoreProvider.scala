package io.liquirium.util.store

import io.liquirium.core.Market

import java.time.Duration

class H2CandleStoreProvider(
  getStoreForMarketStringAndDuration: (String, Duration) => CandleStore,
) extends CandleStoreProvider {

  override def getStore(market: Market, resolution: Duration): CandleStore = {
    val marketString = s"${ market.exchangeId.value }-${ market.tradingPair.base }-${ market.tradingPair.quote }"
    getStoreForMarketStringAndDuration(s"$marketString-${ resolution.getSeconds }", resolution)
  }

}
