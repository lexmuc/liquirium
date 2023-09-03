package io.liquirium.util.store

import io.liquirium.core.Market

import java.time.Duration
import scala.concurrent.Future

trait CandleHistoryLoaderProvider {

  def getHistoryLoader(market: Market, resolution: Duration): Future[CandleHistoryLoader]

}
