package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{candle => bitfinexCandle}
import io.liquirium.core.helpers.CandleHelpers.{ohlc, ohlcCandle}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.{Candle, TradingPair}
import io.liquirium.util.ResultOrder
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern

import java.time.{Duration, Instant}
import scala.util.Failure

class BitfinexApiAdapterTest_GetCandleBatch extends BitfinexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureCandlesRequest(): BitfinexRestApi.GetCandles = {
    val adapterCaptor = argumentCaptor[BitfinexRestApi.GetCandles]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeCandleConversion(bitfinexCandle: BitfinexCandle, candleLength: Duration, genericCandle: Candle) =
    fakeModelConverter.convertCandle(bitfinexCandle, candleLength) returns genericCandle

  private def getCandles(
    tradingPair: TradingPair = defaultPair,
    candleLength: Duration = secs(60),
    start: Instant = sec(0),
  ) = apiAdapter.getCandleBatch(tradingPair, candleLength, start)

  private def replyWith(candles: BitfinexCandle*): Unit = fakeRestApi.completeNext(candles)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  private def c60(start: Instant, n: Int) = ohlcCandle(start = start, length = secs(60), ohlc = ohlc(n))

  test("it issues a request for candles with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    getCandles(tradingPair = pair(123))
    captureCandlesRequest() should matchPattern {
      case cr: BitfinexRestApi.GetCandles if cr.symbol == "BTCUSD" =>
    }
  }

  test("the resolution is translated to the respective bitfinex resolution") {
    getCandles(candleLength = secs(3600))
    captureCandlesRequest() should matchPattern {
      case cr: BitfinexRestApi.GetCandles if cr.candleLength == BitfinexCandleLength.oneHour =>
    }
  }

  test("a failure is returned when no matching bitfinex resolution is found") {
    val f = getCandles(candleLength = secs(123))
    f.value.get should matchPattern { case Failure(_) => }
  }

  test("the start is passed along and the end (until) is None") {
    getCandles(start = sec(3000))
    captureCandlesRequest() should matchPattern {
      case cr: BitfinexRestApi.GetCandles if cr.from.contains(sec(3000)) && cr.until.isEmpty =>
    }
  }

  test("the order is set to ascending") {
    getCandles()
    captureCandlesRequest() should matchPattern {
      case cr: BitfinexRestApi.GetCandles if cr.sort == ResultOrder.AscendingOrder =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxCandleBatchSize = 123
    getCandles()
    captureCandlesRequest() should matchPattern {
      case cr: BitfinexRestApi.GetCandles if cr.limit.contains(123) =>
    }
  }

  test("the returned candles are converted (with the given resolution) and returned in a batch") {
    fakeCandleConversion(bitfinexCandle(1), secs(60), c60(sec(0), 1))
    fakeCandleConversion(bitfinexCandle(2), secs(60), c60(sec(60), 2))
    val f = getCandles(start = sec(0), candleLength = secs(60))
    replyWith(bitfinexCandle(1), bitfinexCandle(2))
    f.value.get.get.candles shouldEqual Seq(c60(sec(0), 1), c60(sec(60), 2))
  }

  test("the batch start is the given start regardless of the first candle start") {
    fakeCandleConversion(bitfinexCandle(1), secs(60), c60(sec(120), 1))
    fakeCandleConversion(bitfinexCandle(2), secs(60), c60(sec(180), 2))
    val f = getCandles(start = sec(60), candleLength = secs(60))
    replyWith(bitfinexCandle(1), bitfinexCandle(2))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("the batch resolution is the given resolution even if the batch is empty") {
    val f = getCandles(start = sec(7200), candleLength = secs(3600))
    replyWith()
    f.value.get.get.start shouldEqual sec(7200)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxCandleBatchSize = 2
    fakeCandleConversion(bitfinexCandle(1), secs(60), c60(sec(120), 1))
    val f = getCandles(start = sec(60), candleLength = secs(60))
    replyWith(bitfinexCandle(1))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start at the end of the latest candle") {
    maxCandleBatchSize = 2
    fakeCandleConversion(bitfinexCandle(1), secs(60), c60(sec(120), 1))
    fakeCandleConversion(bitfinexCandle(2), secs(60), c60(sec(240), 2))
    val f = getCandles(start = sec(60), candleLength = secs(60))
    replyWith(bitfinexCandle(1), bitfinexCandle(2))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(300))
  }

  test("errors are passed along") {
    val f = getCandles()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
