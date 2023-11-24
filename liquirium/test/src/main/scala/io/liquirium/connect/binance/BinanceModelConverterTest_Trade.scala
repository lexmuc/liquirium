package io.liquirium.connect.binance

import io.liquirium.core.{ExchangeId, LedgerRef, StringTradeId}
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{trade => bt}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}


class BinanceModelConverterTest_Trade extends BinanceModelConverterTest {

  private def convert(o: BinanceTrade, exchangeId: ExchangeId = ExchangeId("X")) =
    converter(exchangeId).convertTrade(o)

  test("it assigns the id as a string id and the orderId as a string") {
    convert(bt(id = "ID123")).id shouldEqual StringTradeId("ID123")
    convert(bt(orderId = "O123")).orderId shouldEqual Some("O123")
  }

  test("the market is obtained via the converter itself") {
    convert(bt(symbol = "CADUSD"), ExchangeId("XYZ")).market shouldEqual
      converter(ExchangeId("XYZ")).getMarket("CADUSD")
  }

  test("the price is just assigned") {
    convert(bt(price = dec(123))).price shouldEqual dec(123)
  }

  test("the sign of the quantity is determined by the isBuyer flag") {
    convert(bt(quantity = dec(7), isBuyer = true)).quantity shouldEqual dec(7)
    convert(bt(quantity = dec(7), isBuyer = false)).quantity shouldEqual dec(-7)
  }

  test("the fees are empty if the commission is zero") {
    convert(bt(commission = dec(0))).fees shouldEqual Seq()
  }

  test("the fees only contain one element with the correct ledger when a fee is given") {
    convert(bt(commission = dec(33), commissionAsset = "BNB"), ExchangeId("XYZ")).fees shouldEqual Seq(
      LedgerRef(ExchangeId("XYZ"), "BNB") -> dec(33)
    )
  }

  test("the time is just assigned") {
    convert(bt(time = sec(123))).time shouldEqual sec(123)
  }

}
