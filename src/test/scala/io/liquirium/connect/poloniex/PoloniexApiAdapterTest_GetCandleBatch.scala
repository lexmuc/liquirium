package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetCandles
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCandle
import io.liquirium.core.helpers.CandleHelpers.{ohlc, ohlcCandle}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.{Candle, TradingPair}

import java.time.{Duration, Instant}
import scala.util.Failure

class PoloniexApiAdapterTest_GetCandleBatch extends PoloniexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureCandlesRequest(): GetCandles = {
    val adapterCaptor = argumentCaptor[GetCandles]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeCandleConversion(poloniexCandle: PoloniexCandle, genericCandle: Candle) =
    fakeModelConverter.convertCandle(poloniexCandle) returns genericCandle

  private def getCandles(
    tradingPair: TradingPair = defaultPair,
    interval: Duration = secs(60),
    start: Instant = sec(0),
  ) = apiAdapter.getCandleBatch(tradingPair, interval, start)

  private def replyWith(candles: PoloniexCandle*): Unit = fakeRestApi.completeNext(candles)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  private def c60(start: Instant, n: Int) = ohlcCandle(start = start, length = secs(60), ohlc = ohlc(n))

  test("it issues a request for candles with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    getCandles(tradingPair = pair(123))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.symbol == "BTCUSD" =>
    }
  }

  test("the interval is translated to the respective poloniex candle length") {
    getCandles(interval = secs(3600))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.interval == PoloniexCandleLength.oneHour =>
    }
  }

  test("an exception is thrown when no matching poloniex candle length is found") {
    an[Exception] shouldBe thrownBy(getCandles(interval = secs(123)))
  }

  test("the start is passed along and the end (until) is None") {
    getCandles(start = sec(3000))
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.startTime.contains(sec(3000)) && cr.endTime.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxCandleBatchSize = 123
    getCandles()
    captureCandlesRequest() should matchPattern {
      case cr: GetCandles if cr.limit.contains(123) =>
    }
  }

  test("the returned candles are converted and returned in a batch") {
    fakeCandleConversion(poloniexCandle(1), c60(sec(0), 1))
    fakeCandleConversion(poloniexCandle(2), c60(sec(60), 2))
    val f = getCandles(start = sec(0), interval = secs(60))
    replyWith(poloniexCandle(1), poloniexCandle(2))
    f.value.get.get.candles shouldEqual Seq(c60(sec(0), 1), c60(sec(60), 2))
  }

  test("the batch start is the given start regardless of the first candle start") {
    fakeCandleConversion(poloniexCandle(1), c60(sec(120), 1))
    fakeCandleConversion(poloniexCandle(2), c60(sec(180), 2))
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith(poloniexCandle(1), poloniexCandle(2))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("the batch candle length is the given candle length even if the batch is empty") {
    val f = getCandles(start = sec(7200), interval = secs(3600))
    replyWith()
    f.value.get.get.candleLength shouldEqual secs(3600)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxCandleBatchSize = 2
    fakeCandleConversion(poloniexCandle(1), c60(sec(120), 1))
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith(poloniexCandle(1))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start at the end of the latest candle") {
    maxCandleBatchSize = 2
    fakeCandleConversion(poloniexCandle(1), c60(sec(120), 1))
    fakeCandleConversion(poloniexCandle(2), c60(sec(240), 2))
    val f = getCandles(start = sec(60), interval = secs(60))
    replyWith(poloniexCandle(1), poloniexCandle(2))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(300))
  }

  test("errors are passed along") {
    val f = getCandles()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
