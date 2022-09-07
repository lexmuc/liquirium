package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core.{CandleHistorySegment, TradingPair}


trait ExchangeConnector {

  def candleHistoryStream(
    tradingPair: TradingPair,
    initialSegment: CandleHistorySegment,
  ): Source[CandleHistorySegment, NotUsed]

}
