package io.liquirium.connect.binance

import io.liquirium.core.TradingPair
import io.liquirium.core.helper.TestWithMocks
import io.liquirium.core.helper.async.{AsyncTestWithControlledTime, FutureServiceMock}

class BinanceApiAdapterTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val fakeRestApi = new FutureServiceMock[BinanceRestApi, Any](_.sendRequest(*))
  protected val fakeModelConverter: BinanceModelConverter = mock[BinanceModelConverter]
  protected var maxCandleBatchSize: Int = 10

  protected def fakeTradingPairToSymbolConversion(tradingPair: TradingPair, symbol: String): Unit =
    fakeModelConverter.getSymbol(tradingPair) returns symbol

  lazy val api = new BinanceApiAdapter(
    restApi = fakeRestApi.instance,
    modelConverter = fakeModelConverter,
    maxCandleBatchSize = maxCandleBatchSize,
  )

}
