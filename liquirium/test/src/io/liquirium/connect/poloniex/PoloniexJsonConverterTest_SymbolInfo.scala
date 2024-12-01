package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsValue, Json}

class PoloniexJsonConverterTest_SymbolInfo extends BasicTest {

  private def convertSymbolInfo(json: JsValue) = new PoloniexJsonConverter().convertSymbolInfo(json)

  private def symbolInfoJson(
    symbol: String = "ETHBTC",
    priceScale: Int = 1,
    quantityScale: Int = 1,
    minAmount: String = "1",
    minQuantity: String = "1",
  ) = Json.parse(
    s"""{
       |    "symbol": "$symbol",
       |    "baseCurrencyName": "BTC",
       |    "quoteCurrencyName": "USDT",
       |    "displayName": "BTC/USDT",
       |    "state": "NORMAL",
       |    "visibleStartTime": 1659018819512,
       |    "tradableStartTime": 1659018819512,
       |    "symbolTradeLimit": {
       |      "symbol": "BTC_USDT",
       |      "priceScale": $priceScale,
       |      "quantityScale": $quantityScale,
       |      "amountScale": 2,
       |      "minQuantity": "$minQuantity",
       |      "minAmount": "$minAmount",
       |      "highestBid": "0",
       |      "lowestAsk": "0"
       |    },
       |    "crossMargin": {
       |      "supportCrossMargin": true,
       |      "maxLeverage": 3
       |    }
       |  }""".stripMargin)

  test("it parses the symbol field") {
    convertSymbolInfo(symbolInfoJson(symbol = "LTCBTC")).symbol shouldEqual "LTCBTC"
  }

  test("it parses the price scale field") {
    convertSymbolInfo(symbolInfoJson(priceScale = 2)).priceScale shouldEqual 2
  }

  test("it parses the quantity scale field") {
    convertSymbolInfo(symbolInfoJson(quantityScale = 2)).quantityScale shouldEqual 2
  }

  test("it parses the min amount field") {
    convertSymbolInfo(symbolInfoJson(minAmount = "123.4")).minAmount shouldEqual dec("123.4")
  }

  test("it parses the min quantity field") {
    convertSymbolInfo(symbolInfoJson(minQuantity = "123.4")).minQuantity shouldEqual dec("123.4")
  }

}

