package io.liquirium.connect.binance

import io.liquirium.core.{ExchangeId, LedgerRef, Side, Trade}
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.executionReport
import io.liquirium.core.helper.CoreHelpers.{dec, milli}

class BinanceModelConverterTest_ExtractTrade extends BinanceModelConverterTest {

  private def extract(er: BinanceExecutionReport, exchangeId: ExchangeId = ExchangeId("X")) =
    converter(exchangeId).extractTrade(er)

  private def extractFromTradeReport(er: BinanceExecutionReport, exchangeId: ExchangeId = ExchangeId("X")): Trade =
    converter(exchangeId).extractTrade(makeTradeReport(er)).get

  private def makeTradeReport(er: BinanceExecutionReport) = er.copy(
    currentExecutionType = BinanceExecutionType.TRADE,
    tradeId = if (er.tradeId.isEmpty) Some(1) else er.tradeId
  )

  test("it yields None if the execution type is not trade") {
    extract(executionReport(currentExecutionType = BinanceExecutionType.NEW)) shouldEqual None
    extract(executionReport(currentExecutionType = BinanceExecutionType.EXPIRED)) shouldEqual None
    extract(executionReport(currentExecutionType = BinanceExecutionType.CANCELED)) shouldEqual None
  }

  test("trade id and order id are assigned as a string") {
    extractFromTradeReport(executionReport(tradeId = Some(333))).id shouldEqual "333"
    extractFromTradeReport(executionReport(orderId = 444L)).orderId shouldEqual Some("444")
  }

  test("the market is obtained via the converter itself") {
    extractFromTradeReport(executionReport(symbol = "CADUSD"), ExchangeId("XYZ")).market shouldEqual
      converter(ExchangeId("XYZ")).getMarket("CADUSD")
  }

  test("the price is just assigned") {
    extractFromTradeReport(executionReport(lastExecutedPrice = dec(123))).price shouldEqual dec(123)
  }

  test("the sign of the quantity is determined by the side") {
    extractFromTradeReport(executionReport(lastExecutedQuantity = dec(7), side = Side.Buy))
      .quantity shouldEqual dec(7)
    extractFromTradeReport(executionReport(lastExecutedQuantity = dec(7), side = Side.Sell))
      .quantity shouldEqual dec(-7)
  }

  test("the fees are empty if the commission is zero") {
    extractFromTradeReport(executionReport(commissionAmount = dec(0))).fees shouldEqual Seq()
  }

  test("the fees only contain one element with the correct ledger when a fee is given") {
    val report = executionReport(commissionAmount = dec(33), commissionAsset = Some("BNB"))
    extractFromTradeReport(report, ExchangeId("XYZ")).fees shouldEqual Seq(
      LedgerRef(ExchangeId("XYZ"), "BNB") -> dec(33)
    )
  }

  test("the transaction time is taken as the time") {
    extractFromTradeReport(executionReport(transactionTime = 4567, eventTime = 1)).time shouldEqual milli(4567)
  }

}
