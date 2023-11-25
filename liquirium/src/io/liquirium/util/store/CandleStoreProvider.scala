package io.liquirium.util.store

import io.liquirium.core.Market

import java.time.Duration

trait CandleStoreProvider {

  def getStore(market: Market, resolution: Duration): CandleStore

}
