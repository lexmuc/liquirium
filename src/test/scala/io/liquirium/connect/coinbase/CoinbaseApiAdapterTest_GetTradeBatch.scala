package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetTradeHistory
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.coinbaseTrade
import io.liquirium.core.helpers.CoreHelpers.{ex, milli, sec}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core.{Trade, TradingPair}

import java.time.Instant
import scala.util.Failure

class CoinbaseApiAdapterTest_GetTradeBatch extends CoinbaseApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToProductIdConversion(defaultPair, "DEFAULT")

  private def captureTradesRequest(): GetTradeHistory = {
    val adapterCaptor = argumentCaptor[GetTradeHistory]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeTradeConversion(coinbaseTrade: CoinbaseTrade, genericTrade: Trade) =
    fakeModelConverter.convertTrade(coinbaseTrade) returns genericTrade

  private def getTrades(
    tradingPair: TradingPair = defaultPair,
    start: Instant = sec(0),
  ) = apiAdapter.getTradeBatch(tradingPair, start)

  private def replyWith(trades: CoinbaseTrade*): Unit = fakeRestApi.completeNext(trades)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for trades with the given converted trading pair") {
    fakeTradingPairToProductIdConversion(pair(123), "BTC-USD")
    getTrades(tradingPair = pair(123))
    captureTradesRequest() should matchPattern {
      case th: GetTradeHistory if th.productId.contains("BTC-USD") =>
    }
  }

  test("the start is passed along and the endTime (until) is None") {
    getTrades(start = milli(123))
    captureTradesRequest() should matchPattern {
      case th: GetTradeHistory if th.startSequenceTimestamp.contains(milli(123)) && th.endSequenceTimestamp.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxTradeBatchSize = 234
    getTrades()
    captureTradesRequest() should matchPattern {
      case th: GetTradeHistory if th.limit.contains(234) =>
    }
  }

  test("the order id is None") {
    getTrades()
    captureTradesRequest() should matchPattern {
      case th: GetTradeHistory if th.orderId.isEmpty =>
    }
  }

  test("the returned trades are converted, sorted and returned in a batch") {
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(1), "A"))
    fakeTradeConversion(coinbaseTrade("2"), trade(sec(2), "B"))
    fakeTradeConversion(coinbaseTrade("3"), trade(sec(2), "C"))
    fakeTradeConversion(coinbaseTrade("4"), trade(sec(2), "D"))
    val f = getTrades(start = sec(0))
    replyWith(
      coinbaseTrade("1"),
      coinbaseTrade("4"),
      coinbaseTrade("2"),
      coinbaseTrade("3"),
    )
    f.value.get.get.trades shouldEqual Seq(
      trade(sec(1), "A"),
      trade(sec(2), "B"),
      trade(sec(2), "C"),
      trade(sec(2), "D"),
    )
  }

  test("the batch start is the given start regardless of the first trade start") {
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(coinbaseTrade("2"), trade(sec(62), "B"))
    val f = getTrades(start = sec(60))
    replyWith(coinbaseTrade("1"), coinbaseTrade("2"))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxTradeBatchSize = 2
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(61), "A"))
    val f = getTrades(start = sec(60))
    replyWith(coinbaseTrade("1"))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start with the timestamp of the latest trade (yes, overlap!)") {
    maxTradeBatchSize = 2
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(coinbaseTrade("2"), trade(sec(62), "B"))
    val f = getTrades(start = sec(60))
    replyWith(coinbaseTrade("1"), coinbaseTrade("2"))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(62))
  }

  test("a failure is returned when the batch is full and all trades have the same timestamp") {
    maxTradeBatchSize = 2
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(coinbaseTrade("2"), trade(sec(61), "B"))
    val f = getTrades(start = sec(60))
    replyWith(coinbaseTrade("1"), coinbaseTrade("2"))
    f.value.get.isFailure shouldBe true
  }

  test("no exception is thrown when the batch is not full and all trades have the same timestamp") {
    maxTradeBatchSize = 3
    fakeTradeConversion(coinbaseTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(coinbaseTrade("2"), trade(sec(61), "B"))
    val f = getTrades(start = sec(60))
    replyWith(coinbaseTrade("1"), coinbaseTrade("2"))
    f.value.get.isSuccess shouldBe true
  }

  test("errors are passed along") {
    val f = getTrades()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
