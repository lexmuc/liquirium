package io.liquirium.connect.binance

import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{trade => binanceTrade}
import io.liquirium.core.helpers.CoreHelpers.{ex, milli, sec}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core.{Trade, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern

import java.time.Instant
import scala.util.Failure

class BinanceApiAdapterTest_GetTradeBatch extends BinanceApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureTradesRequest(): BinanceRestApi.GetTradesRequest = {
    val adapterCaptor = argumentCaptor[BinanceRestApi.GetTradesRequest]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeTradeConversion(binanceTrade: BinanceTrade, genericTrade: Trade) =
    fakeModelConverter.convertTrade(binanceTrade) returns genericTrade

  private def getTrades(
    tradingPair: TradingPair = defaultPair,
    start: Instant = sec(0),
  ) = apiAdapter.getTradeBatch(tradingPair, start)

  private def replyWith(trades: BinanceTrade*): Unit = fakeRestApi.completeNext(trades)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for trades with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    getTrades(tradingPair = pair(123))
    captureTradesRequest() should matchPattern {
      case cr: BinanceRestApi.GetTradesRequest if cr.symbol == "BTCUSD" =>
    }
  }

  test("the start is passed along and the endTime (until) is None") {
    getTrades(start = milli(123))
    captureTradesRequest() should matchPattern {
      case cr: BinanceRestApi.GetTradesRequest if cr.startTime.contains(milli(123)) && cr.endTime.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxTradeBatchSize = 234
    getTrades()
    captureTradesRequest() should matchPattern {
      case cr: BinanceRestApi.GetTradesRequest if cr.limit.contains(234) =>
    }
  }

  test("the returned trades are converted, sorted and returned in a batch") {
    fakeTradeConversion(binanceTrade("1"), trade(sec(1), "A"))
    fakeTradeConversion(binanceTrade("2"), trade(sec(2), "B"))
    fakeTradeConversion(binanceTrade("3"), trade(sec(2), "C"))
    fakeTradeConversion(binanceTrade("4"), trade(sec(2), "D"))
    val f = getTrades(start = sec(0))
    replyWith(
      binanceTrade("1"),
      binanceTrade("4"),
      binanceTrade("2"),
      binanceTrade("3"),
    )
    f.value.get.get.trades shouldEqual Seq(
      trade(sec(1), "A"),
      trade(sec(2), "B"),
      trade(sec(2), "C"),
      trade(sec(2), "D"),
    )
  }

  test("the batch start is the given start regardless of the first candle start") {
    fakeTradeConversion(binanceTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(binanceTrade("2"), trade(sec(62), "B"))
    val f = getTrades(start = sec(60))
    replyWith(binanceTrade("1"), binanceTrade("2"))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxTradeBatchSize = 2
    fakeTradeConversion(binanceTrade("1"), trade(sec(61), "A"))
    val f = getTrades(start = sec(60))
    replyWith(binanceTrade("1"))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start with the timestamp of the latest trade (yes, overlap!)") {
    maxTradeBatchSize = 2
    fakeTradeConversion(binanceTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(binanceTrade("2"), trade(sec(62), "B"))
    val f = getTrades(start = sec(60))
    replyWith(binanceTrade("1"), binanceTrade("2"))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(62))
  }

  test("a failure is returned when the batch is full and all trades have the same timestamp") {
    maxTradeBatchSize = 2
    fakeTradeConversion(binanceTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(binanceTrade("2"), trade(sec(61), "B"))
    val f = getTrades(start = sec(60))
    replyWith(binanceTrade("1"), binanceTrade("2"))
    f.value.get.isFailure shouldBe true
  }

  test("no exception is thrown when the batch is not full and all trades have the same timestamp") {
    maxTradeBatchSize = 3
    fakeTradeConversion(binanceTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(binanceTrade("2"), trade(sec(61), "B"))
    val f = getTrades(start = sec(60))
    replyWith(binanceTrade("1"), binanceTrade("2"))
    f.value.get.isSuccess shouldBe true
  }

  test("errors are passed along") {
    val f = getTrades()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
