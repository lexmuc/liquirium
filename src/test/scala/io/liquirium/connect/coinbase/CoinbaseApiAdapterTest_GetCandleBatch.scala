package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetCandles
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.coinbaseCandle
import io.liquirium.core.helpers.CandleHelpers.{ohlc, ohlcCandle}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.{Candle, TradingPair}

import java.time.{Duration, Instant}
import scala.util.Failure

class CoinbaseApiAdapterTest_GetCandleBatch extends CoinbaseApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToProductIdConversion(defaultPair, "DEFAULT")

  private def captureCandlesRequest(): GetCandles = {
    val adapterCaptor = argumentCaptor[GetCandles]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeCandleConversion(coinbaseCandle: CoinbaseCandle, candleLength: Duration, genericCandle: Candle) =
    fakeModelConverter.convertCandle(coinbaseCandle, candleLength) returns genericCandle

  private def getCandles(
    tradingPair: TradingPair = defaultPair,
    interval: Duration = secs(60),
    start: Instant = sec(0),
  ) = apiAdapter.getCandleBatch(tradingPair, interval, start)

  private def replyWith(candles: CoinbaseCandle*): Unit = fakeRestApi.completeNext(candles)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  private def c60(start: Instant, n: Int) = ohlcCandle(start = start, length = secs(60), ohlc = ohlc(n))

  test("it issues a request for candles with the given converted trading pair") {
    fakeTradingPairToProductIdConversion(pair(123), "BTCUSD")
    getCandles(tradingPair = pair(123))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.productId == "BTCUSD" =>
    }
  }

  test("the interval is translated to the respective coinbase candle length") {
    getCandles(interval = secs(3600))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.granularity == CoinbaseCandleLength.oneHour =>
    }
  }

  test("a failure is returned when no matching coinbase candle length is found") {
    val f = getCandles(interval = secs(123))
    f.value.get should matchPattern { case Failure(_) => }
  }

  test("the start is passed along and the end is the start" +
    "plus the product of max candle batch size and candle length minus one second") {
    maxCandleBatchSize = 10
    getCandles(start = sec(3000), interval = secs(60))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.start == sec(3000) && cr.end == sec(3599) =>
    }
  }

  test("the returned candles are converted and returned in a batch") {
    fakeCandleConversion(coinbaseCandle(1), secs(60), c60(sec(0), 1))
    fakeCandleConversion(coinbaseCandle(2), secs(60), c60(sec(60), 2))
    val f = getCandles(start = sec(0), interval = secs(60))
    replyWith(coinbaseCandle(1), coinbaseCandle(2))
    f.value.get.get.candles shouldEqual Seq(c60(sec(0), 1), c60(sec(60), 2))
  }

  test("the batch start is the given start regardless of the first candle start") {
    fakeCandleConversion(coinbaseCandle(1), secs(60), c60(sec(120), 1))
    fakeCandleConversion(coinbaseCandle(2), secs(60), c60(sec(180), 2))
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith(coinbaseCandle(1), coinbaseCandle(2))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("the batch candle length is the given candle length even if the batch is empty") {
    val f = getCandles(start = sec(7200), interval = secs(3600))
    replyWith()
    f.value.get.get.candleLength shouldEqual secs(3600)
  }

  test("the next batch start is set to the start time" +
    "plus the product of max candle batch size and candle length") {
    maxCandleBatchSize = 2
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith()
    f.value.get.get.nextBatchStart shouldEqual Some(sec(180))
  }

  test("there is no next batch if the nextBatchStart would be now or later") {
    maxCandleBatchSize = 1
    clock.set(Instant.ofEpochSecond(61))
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith()
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("errors are passed along") {
    val f = getCandles()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
