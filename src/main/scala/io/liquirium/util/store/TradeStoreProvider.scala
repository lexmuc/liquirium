package io.liquirium.util.store

import io.liquirium.core.Market

trait TradeStoreProvider {

  def getStore(market: Market): TradeStore

}
