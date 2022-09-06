package io.liquirium.connect

import io.liquirium.core.TradingPair

import java.time.{Duration, Instant}
import scala.concurrent.Future

trait GenericExchangeApi {

  def getForwardCandleBatch(
    tradingPair: TradingPair,
    resolution: Duration,
    start: Instant,
  ): Future[ForwardCandleBatch]

}
