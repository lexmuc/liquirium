package io.liquirium.util.store

import io.liquirium.core.{Market, TradeHistoryLoader}

import scala.concurrent.Future

trait TradeHistoryLoaderProvider {

  def getHistoryLoader(market: Market): Future[TradeHistoryLoader]

}
