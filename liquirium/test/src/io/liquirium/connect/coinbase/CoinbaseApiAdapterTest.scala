package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Market, OrderConstraints, TradingPair}
import io.liquirium.helpers.FakeClock
import org.mockito.Mockito.mock

import java.time.Instant

class CoinbaseApiAdapterTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val fakeRestApi = new FutureServiceMock[CoinbaseRestApi, Any](_.sendRequest(*))
  protected val fakeModelConverter: CoinbaseModelConverter = mock(classOf[CoinbaseModelConverter])
  protected var maxCandleBatchSize: Int = 10
  protected var maxTradeBatchSize: Int = 10
  protected var maxOrderBatchSize: Int = 10
  protected val clock: FakeClock = FakeClock(Instant.now())

  protected def fakeTradingPairToProductIdConversion(tradingPair: TradingPair, productId: String): Unit =
    fakeModelConverter.getProductId(tradingPair) returns productId

  protected def fakeProductInfoConversion(
    coinbaseProductInfo: CoinbaseProductInfo,
    orderConstraints: OrderConstraints,
  ): Unit =
    fakeModelConverter.convertProductInfo(coinbaseProductInfo) returns orderConstraints

  protected def fakeSymbolToMarketConversion(symbol: String, market: Market): Unit =
    fakeModelConverter.getMarket(symbol) returns market

  lazy val apiAdapter = new CoinbaseApiAdapter(
    restApi = fakeRestApi.instance,
    modelConverter = fakeModelConverter,
    maxCandleBatchSize = maxCandleBatchSize,
    maxTradeBatchSize = maxTradeBatchSize,
    maxOrderBatchSize = maxOrderBatchSize,
    clock = clock,
  )

}
