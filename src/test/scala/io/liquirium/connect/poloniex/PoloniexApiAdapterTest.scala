package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Market, OrderConstraints, TradingPair}

class PoloniexApiAdapterTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val fakeRestApi = new FutureServiceMock[PoloniexRestApi, Any](_.sendRequest(*))
  protected val fakeModelConverter: PoloniexModelConverter = mock[PoloniexModelConverter]
  protected var maxCandleBatchSize: Int = 10
  protected var maxTradeBatchSize: Int = 10
  protected var maxOrderBatchSize: Int = 10

  protected def fakeTradingPairToSymbolConversion(tradingPair: TradingPair, symbol: String): Unit =
    fakeModelConverter.getSymbol(tradingPair) returns symbol

  protected def fakeSymbolInfoConversion(
    poloniexSymbolInfo: PoloniexSymbolInfo,
    orderConstraints: OrderConstraints,
  ): Unit =
    fakeModelConverter.convertSymbolInfo(poloniexSymbolInfo) returns orderConstraints

  protected def fakeSymbolToMarketConversion(symbol: String, market: Market): Unit =
    fakeModelConverter.getMarket(symbol) returns market

  lazy val apiAdapter = new PoloniexApiAdapter(
    restApi = fakeRestApi.instance,
    modelConverter = fakeModelConverter,
    maxCandleBatchSize = maxCandleBatchSize,
    maxTradeBatchSize = maxTradeBatchSize,
    maxOrderBatchSize = maxOrderBatchSize,
  )

}
