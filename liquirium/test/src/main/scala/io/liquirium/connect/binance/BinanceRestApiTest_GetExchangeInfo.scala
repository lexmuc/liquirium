package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.PublicGet
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.symbolInfo
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsValue, Json}

class BinanceRestApiTest_GetExchangeInfo extends BinanceRestApiTest {

  private def getExchangeInfo = BinanceRestApi.GetExchangeInfo()

  private def fakeConversion(json: JsValue, symbolInfos: BinanceSymbolInfo*) =
    jsonConverter.convertSymbolInfos(json) returns symbolInfos.toSeq

  test("requesting exchange info yields a get request with the proper url") {
    getExchangeInfo.httpRequest(converter = jsonConverter) should matchPattern { case PublicGet("/api/v3/exchangeInfo", _) => }
  }

  test("the response is converted to binance symbol infos") {
    val response = Json.parse(
      s"""
         |{"symbols" : ${ json(123).toString() }}
         |""".stripMargin)
    fakeConversion(json(123), symbolInfo())
    getExchangeInfo.convertResponse(response, jsonConverter) shouldEqual Seq(symbolInfo())
  }

}
