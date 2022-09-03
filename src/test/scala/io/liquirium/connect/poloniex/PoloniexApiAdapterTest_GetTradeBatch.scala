package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetTradeHistory
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexTrade
import io.liquirium.core.helpers.CoreHelpers.{ex, milli, sec}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core.{Trade, TradingPair}

import java.time.Instant
import scala.util.Failure

class PoloniexApiAdapterTest_GetTradeBatch extends PoloniexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureTradesRequest(): GetTradeHistory = {
    val adapterCaptor = argumentCaptor[GetTradeHistory]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeTradeConversion(poloniexTrade: PoloniexTrade, genericTrade: Trade) =
    fakeModelConverter.convertTrade(poloniexTrade) returns genericTrade

  private def getTradeBatch(
    tradingPair: TradingPair = defaultPair,
    start: Instant = sec(0),
  ) = apiAdapter.getTradeBatch(tradingPair, start)

  private def replyWith(trades: PoloniexTrade*): Unit = fakeRestApi.completeNext(trades)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for trades with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTC-USD")
    getTradeBatch(tradingPair = pair(123))
    captureTradesRequest() should matchPattern {
      case gth: GetTradeHistory if gth.symbols.contains("BTC-USD") =>
    }
  }

  test("the start is passed along and the endTime (until) is None") {
    getTradeBatch(start = milli(123))
    captureTradesRequest() should matchPattern {
      case gth: GetTradeHistory if gth.startTime.contains(milli(123)) && gth.endTime.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxTradeBatchSize = 234
    getTradeBatch()
    captureTradesRequest() should matchPattern {
      case gth: GetTradeHistory if gth.limit.contains(234) =>
    }
  }

  test("the from field is None") {
    getTradeBatch()
    captureTradesRequest() should matchPattern {
      case gth: GetTradeHistory if gth.from.isEmpty =>
    }
  }

  test("the direction field is None") {
    getTradeBatch()
    captureTradesRequest() should matchPattern {
      case gth: GetTradeHistory if gth.direction.isEmpty =>
    }
  }

  test("the returned trades are converted, sorted and returned in a batch") {
    fakeTradeConversion(poloniexTrade("1"), trade(sec(1), "A"))
    fakeTradeConversion(poloniexTrade("2"), trade(sec(2), "B"))
    fakeTradeConversion(poloniexTrade("3"), trade(sec(2), "C"))
    fakeTradeConversion(poloniexTrade("4"), trade(sec(2), "D"))
    val f = getTradeBatch(start = sec(0))
    replyWith(
      poloniexTrade("1"),
      poloniexTrade("4"),
      poloniexTrade("2"),
      poloniexTrade("3"),
    )
    f.value.get.get.trades shouldEqual Seq(
      trade(sec(1), "A"),
      trade(sec(2), "B"),
      trade(sec(2), "C"),
      trade(sec(2), "D"),
    )
  }

  test("the batch start is the given start regardless of the first trade start") {
    fakeTradeConversion(poloniexTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(poloniexTrade("2"), trade(sec(62), "B"))
    val f = getTradeBatch(start = sec(60))
    replyWith(poloniexTrade("1"), poloniexTrade("2"))
    f.value.get.get.start shouldEqual sec(60)
  }

  test("there is no next batch if the batch size limit was not reached") {
    maxTradeBatchSize = 2
    fakeTradeConversion(poloniexTrade("1"), trade(sec(61), "A"))
    val f = getTradeBatch(start = sec(60))
    replyWith(poloniexTrade("1"))
    f.value.get.get.nextBatchStart shouldEqual None
  }

  test("a full batch (limit reach) has a next batch start with the timestamp of the latest trade (yes, overlap!)") {
    maxTradeBatchSize = 2
    fakeTradeConversion(poloniexTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(poloniexTrade("2"), trade(sec(62), "B"))
    val f = getTradeBatch(start = sec(60))
    replyWith(poloniexTrade("1"), poloniexTrade("2"))
    f.value.get.get.nextBatchStart shouldEqual Some(sec(62))
  }

  test("a failure is returned when the batch is full and all trades have the same timestamp") {
    maxTradeBatchSize = 2
    fakeTradeConversion(poloniexTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(poloniexTrade("2"), trade(sec(61), "B"))
    val f = getTradeBatch(start = sec(60))
    replyWith(poloniexTrade("1"), poloniexTrade("2"))
    f.value.get.isFailure shouldBe true
  }

  test("no exception is thrown when the batch is not full and all trades have the same timestamp") {
    maxTradeBatchSize = 3
    fakeTradeConversion(poloniexTrade("1"), trade(sec(61), "A"))
    fakeTradeConversion(poloniexTrade("2"), trade(sec(61), "B"))
    val f = getTradeBatch(start = sec(60))
    replyWith(poloniexTrade("1"), poloniexTrade("2"))
    f.value.get.isSuccess shouldBe true
  }

  test("errors are passed along") {
    val f = getTradeBatch()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
