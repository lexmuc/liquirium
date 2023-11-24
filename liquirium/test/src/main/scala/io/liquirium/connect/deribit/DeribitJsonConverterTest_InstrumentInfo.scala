package io.liquirium.connect.deribit

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import play.api.libs.json.{JsValue, Json}

class DeribitJsonConverterTest_InstrumentInfo extends BasicTest {

  private def convert(json: JsValue) = new DeribitJsonConverter().convertInstrumentInfo(json)

  private def infoJson(
    instrumentName: String = "ABC",
    tickSize: String = "1",
    minTradeAmount: String = "1",
    contractSize: String = "1",
  ) = Json.parse(
    s"""
       |{
       |      "tick_size": $tickSize,
       |      "taker_commission": 0.0005,
       |      "settlement_period": "month",
       |      "settlement_currency": "BTC",
       |      "rfq": false,
       |      "quote_currency": "USD",
       |      "price_index": "btc_usd",
       |      "min_trade_amount": $minTradeAmount,
       |      "max_liquidation_commission": 0.0075,
       |      "max_leverage": 50,
       |      "maker_commission": 0,
       |      "kind": "future",
       |      "is_active": true,
       |      "instrument_name": "$instrumentName",
       |      "instrument_id": 138583,
       |      "future_type": "reversed",
       |      "expiration_timestamp": 1695974400000,
       |      "creation_timestamp": 1664524802000,
       |      "counter_currency": "USD",
       |      "contract_size": $contractSize,
       |      "block_trade_tick_size": 0.01,
       |      "block_trade_min_trade_amount": 200000,
       |      "block_trade_commission": 0.00025,
       |      "base_currency": "BTC"
       |    }
       |""".stripMargin
  )

  test("it parses the symbol field") {
    convert(infoJson(instrumentName = "LTCBTC")).instrumentName shouldEqual "LTCBTC"
  }

  test("it parses the tick size field") {
    convert(infoJson(tickSize = "123.4")).tickSize shouldEqual dec("123.4")
  }

  test("it parses the min trade amount field") {
    convert(infoJson(minTradeAmount = "123.4")).minTradeAmount shouldEqual dec("123.4")
  }

  test("it parses the contract size field") {
    convert(infoJson(contractSize = "123.4")).contractSize shouldEqual dec("123.4")
  }

}
