package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core.{CandleHistorySegment, TradingPair}

import java.time.{Duration, Instant}

trait ExchangeConnector {

  def candleHistoryStream(
    tradingPair: TradingPair,
    resolution: Duration,
    start: Instant,
  ): Source[CandleHistorySegment, NotUsed]

}
