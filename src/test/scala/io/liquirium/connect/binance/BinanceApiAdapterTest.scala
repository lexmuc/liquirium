package io.liquirium.connect.binance

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Market, OrderConstraints, TradingPair}

class BinanceApiAdapterTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val fakeRestApi = new FutureServiceMock[BinanceRestApi, Any](_.sendRequest(*))
  protected val fakeModelConverter: BinanceModelConverter = mock[BinanceModelConverter]
  protected var maxCandleBatchSize: Int = 10
  protected var maxTradeBatchSize: Int = 10

  protected def fakeTradingPairToSymbolConversion(tradingPair: TradingPair, symbol: String): Unit =
    fakeModelConverter.getSymbol(tradingPair) returns symbol

  protected def fakeSymbolInfoConversion(
    binanceSymbolInfo: BinanceSymbolInfo,
    orderConstraints: OrderConstraints,
  ): Unit =
    fakeModelConverter.convertSymbolInfo(binanceSymbolInfo) returns orderConstraints

  protected def fakeSymbolToMarketConversion(symbol: String, market: Market): Unit =
    fakeModelConverter.getMarket(symbol) returns market

  lazy val apiAdapter = new BinanceApiAdapter(
    restApi = fakeRestApi.instance,
    modelConverter = fakeModelConverter,
    maxCandleBatchSize = maxCandleBatchSize,
    maxTradeBatchSize = maxTradeBatchSize,
  )

}
