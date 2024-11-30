package io.liquirium.connect.binance

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import play.api.libs.json.{JsValue, Json}

class BinanceJsonConverterTest_SymbolInfo extends BasicTest {

  private def convertSymbolInfo(json: JsValue) = new BinanceJsonConverter().convertSymbolInfo(json)

  private def symbolInfoJson(
    symbol: String = "ETHBTC",
    baseAsset: String = "ADA",
    quoteAsset: String = "USD",
    tickSize: String = "1",
    stepSize: String = "1",
    minPrice: String = "1",
    maxPrice: String = "1",
    minQuantity: String = "1",
    maxQuantity: String = "1",
  ) = Json.parse(
    s"""{
       |"symbol": "$symbol",
       |"baseAsset": "$baseAsset",
       |"quoteAsset": "$quoteAsset",
       |"filters": [
       |{"filterType": "PRICE_FILTER", "tickSize": "$tickSize", "minPrice": "$minPrice", "maxPrice": "$maxPrice"},
       |{"filterType": "LOT_SIZE", "stepSize": "$stepSize", "minQty": "$minQuantity", "maxQty": "$maxQuantity"}
       |]
       |}""".stripMargin)


  test("it parses the symbol field with base and quote asset") {
    val infoJson = symbolInfoJson(
      symbol = "LTCBTC",
      baseAsset = "MANA",
      quoteAsset = "ETH",
    )
    convertSymbolInfo(infoJson).symbol shouldEqual "LTCBTC"
    convertSymbolInfo(infoJson).baseAsset shouldEqual "MANA"
    convertSymbolInfo(infoJson).quoteAsset shouldEqual "ETH"
  }

  test("it parses the tick size") {
    convertSymbolInfo(symbolInfoJson(tickSize = "0.001")).tickSize shouldEqual dec("0.001")
  }

  test("it parses the min price") {
    convertSymbolInfo(symbolInfoJson(minPrice = "1.1")).minPrice shouldEqual dec("1.1")
  }

  test("it parses the max price") {
    convertSymbolInfo(symbolInfoJson(maxPrice = "1000.1")).maxPrice shouldEqual dec("1000.1")
  }

  test("it parses the step size") {
    convertSymbolInfo(symbolInfoJson(stepSize = "0.001")).stepSize shouldEqual dec("0.001")
  }

  test("it parses the min quantity") {
    convertSymbolInfo(symbolInfoJson(minQuantity = "0.001")).minQuantity shouldEqual dec("0.001")
  }

  test("it parses the max quantity") {
    convertSymbolInfo(symbolInfoJson(maxQuantity = "1000.1")).maxQuantity shouldEqual dec("1000.1")
  }

}
