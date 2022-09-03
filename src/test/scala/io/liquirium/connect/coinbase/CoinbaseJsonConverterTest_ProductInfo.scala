package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import play.api.libs.json.{JsValue, Json}

class CoinbaseJsonConverterTest_ProductInfo extends BasicTest {

  private def convert(json: JsValue) = new CoinbaseJsonConverter().convertProduct(json)

  private def infoJson(
    symbol: String = "ABC",
    baseIncrement: String = "1",
    baseMinSize: String = "1",
    baseMaxSize: String = "1",
    quoteIncrement: String = "1",
    quoteMinSize: String = "1",
    quoteMaxSize: String = "1",
  ) =
    Json.parse(
      s"""{
         |"product_id":"$symbol",
         |"price":"27658.44",
         |"price_percentage_change_24h":"-2.48225912623266",
         |"volume_24h":"23121.68958911",
         |"volume_percentage_change_24h":"-33.50823457933391",
         |"base_increment":"$baseIncrement",
         |"quote_increment":"$quoteIncrement",
         |"quote_min_size":"$quoteMinSize",
         |"quote_max_size":"$quoteMaxSize",
         |"base_min_size":"$baseMinSize",
         |"base_max_size":"$baseMaxSize",
         |"base_name":"Bitcoin",
         |"quote_name":"US Dollar",
         |"watched":false,
         |"is_disabled":false,
         |"new":false,"status":"online",
         |"cancel_only":false,
         |"limit_only":false,
         |"post_only":false,
         |"trading_disabled":false,
         |"auction_mode":false,
         |"product_type":"SPOT",
         |"quote_currency_id":"USD",
         |"base_currency_id":"BTC",
         |"fcm_trading_session_details":null,
         |"mid_market_price":"",
         |"alias":"",
         |"alias_to":[],
         |"base_display_symbol":"BTC",
         |"quote_display_symbol":"USD"
         |}""".stripMargin)

  test("it parses the symbol field") {
    convert(infoJson(symbol = "LTCBTC")).symbol shouldEqual "LTCBTC"
  }

  test("it parses the base increment field") {
    convert(infoJson(baseIncrement = "0.00000002")).baseIncrement shouldEqual dec("0.00000002")
  }

  test("it parses the base min size field") {
    convert(infoJson(baseMinSize = "0.000011")).baseMinSize shouldEqual dec("0.000011")
  }

  test("it parses the base max size field") {
    convert(infoJson(baseMaxSize = "2700")).baseMaxSize shouldEqual dec("2700")
  }

  test("it parses the quote increment field") {
    convert(infoJson(quoteIncrement = "0.02")).quoteIncrement shouldEqual dec("0.02")
  }

  test("it parses the quote min size field") {
    convert(infoJson(quoteMinSize = "2")).quoteMinSize shouldEqual dec("2")
  }

  test("it parses the quote max size field") {
    convert(infoJson(quoteMaxSize = "5000000")).quoteMaxSize shouldEqual dec("5000000")
  }

}
