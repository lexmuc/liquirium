package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Market, OrderConstraints, TradingPair}
import org.mockito.Mockito.mock

class BitfinexApiAdapterTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val fakeRestApi = new FutureServiceMock[BitfinexRestApi, Any](_.sendRequest(*))
  protected val fakeModelConverter: BitfinexModelConverter = mock(classOf[BitfinexModelConverter])
  protected var maxCandleBatchSize: Int = 10
  protected var maxTradeBatchSize: Int = 10
  protected var maxOrderBatchSize: Int = 10

  protected def fakeTradingPairToSymbolConversion(tradingPair: TradingPair, symbol: String): Unit =
    fakeModelConverter.getSymbol(tradingPair) returns symbol

  protected def fakePairInfoConversion(
    bitfinexPairInfo: BitfinexPairInfo,
    orderConstraints: OrderConstraints,
  ): Unit =
    fakeModelConverter.convertPairInfo(bitfinexPairInfo) returns orderConstraints

  protected def fakePairToMarketConversion(pair: String, market: Market): Unit =
    fakeModelConverter.getMarketFromPair(pair) returns market

  lazy val apiAdapter = new BitfinexApiAdapter(
    restApi = fakeRestApi.instance,
    modelConverter = fakeModelConverter,
    maxCandleBatchSize = maxCandleBatchSize,
    maxTradeBatchSize = maxTradeBatchSize,
    maxOrderHistoryBatchSize = maxOrderBatchSize
  )

}
