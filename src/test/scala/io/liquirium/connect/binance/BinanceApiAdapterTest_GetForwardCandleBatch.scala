package io.liquirium.connect.binance

import io.liquirium.connect.AscendingHistoryBatch
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{candle => binanceCandle}
import io.liquirium.core.helper.CandleHelpers.{ohlc, ohlcCandle}
import io.liquirium.core.helper.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helper.MarketHelpers.pair
import io.liquirium.core.{Candle, TradingPair}

import java.time.{Duration, Instant}
import scala.util.Failure

class BinanceApiAdapterTest_GetForwardCandleBatch extends BinanceApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureCandlesRequest(): BinanceRestApi.CandlesRequest = {
    val adapterCaptor = argumentCaptor[BinanceRestApi.CandlesRequest]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeCandleConversion(binanceCandle: BinanceCandle, genericCandle: Candle) =
    fakeModelConverter.convertCandle(binanceCandle) returns genericCandle

  private def getCandles(
    tradingPair: TradingPair = defaultPair,
    resolution: Duration = secs(60),
    start: Instant = sec(0),
  ) = api.getForwardCandleBatch(tradingPair, resolution, start)

  private def replyWith(candles: BinanceCandle*): Unit =
    fakeRestApi.completeNext(AscendingHistoryBatch.apply(candles))

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  private def c60(start: Instant, n: Int) = ohlcCandle(start = start, length = secs(60), ohlc = ohlc(n))

  test("it issues a request for candles with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    getCandles(tradingPair = pair(123))
    captureCandlesRequest() should matchPattern {
      case cr: BinanceRestApi.CandlesRequest if cr.symbol == "BTCUSD" =>
    }
  }

  test("the resolution is translated to the respective binance resolution") {
    getCandles(resolution = secs(3600))
    captureCandlesRequest() should matchPattern {
      case cr: BinanceRestApi.CandlesRequest if cr.resolution == BinanceCandleResolution.oneHour =>
    }
  }

  test("a failure is returned when no matching binance resolution is found") {
    val f = getCandles(resolution = secs(123))
    f.value.get should matchPattern { case Failure(_) => }
  }

  test("the start is passed along and the end (until) is None") {
    getCandles(start = sec(3000))
    captureCandlesRequest() should matchPattern {
      case cr: BinanceRestApi.CandlesRequest if cr.from.contains(sec(3000)) && cr.until.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxCandleBatchSize = 123
    getCandles()
    captureCandlesRequest() should matchPattern {
      case cr: BinanceRestApi.CandlesRequest if cr.limit.contains(123) =>
    }
  }

  test("the returned candles are converted and returned in a batch") {
    fakeCandleConversion(binanceCandle(1), c60(sec(0), 1))
    fakeCandleConversion(binanceCandle(2), c60(sec(60), 2))
    val f = getCandles(start = sec(0), resolution = secs(60))
    replyWith(binanceCandle(1), binanceCandle(2))
    f.value.get.get.candles shouldEqual Seq(c60(sec(0), 1), c60(sec(60), 2))
  }

  test("the batch start is the given start regardless of the first candle start") {
    fakeCandleConversion(binanceCandle(1), c60(sec(120), 1))
    fakeCandleConversion(binanceCandle(2), c60(sec(180), 2))
    val f = getCandles(start = sec(60), resolution = secs(60))
    replyWith(binanceCandle(1), binanceCandle(2))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("the batch resolution is the given resolution even if the batch is empty") {
    val f = getCandles(start = sec(7200), resolution = secs(3600))
    replyWith()
    f.value.get.get.start shouldEqual sec(7200)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxCandleBatchSize = 2
    fakeCandleConversion(binanceCandle(1), c60(sec(120), 1))
    val f = getCandles(start = sec(60), resolution = secs(60))
    replyWith(binanceCandle(1))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start at the end of the latest candle") {
    maxCandleBatchSize = 2
    fakeCandleConversion(binanceCandle(1), c60(sec(120), 1))
    fakeCandleConversion(binanceCandle(2), c60(sec(240), 2))
    val f = getCandles(start = sec(60), resolution = secs(60))
    replyWith(binanceCandle(1), binanceCandle(2))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(300))
  }

  test("errors are passed along") {
    val f = getCandles()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
