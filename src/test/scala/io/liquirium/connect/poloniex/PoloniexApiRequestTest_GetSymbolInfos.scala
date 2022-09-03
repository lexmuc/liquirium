package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetSymbolInfos
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PublicGet
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.symbolInfo
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsValue, Json}

class PoloniexApiRequestTest_GetSymbolInfos extends TestWithMocks {

  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  private def getSymbolInfos = GetSymbolInfos()

  private def fakeConversion(json: JsValue, symbolInfos: PoloniexSymbolInfo*) =
    jsonConverter.convertSymbolInfos(json) returns symbolInfos.toSeq

  test("requesting symbol infos yields a get request with the proper url") {
    getSymbolInfos.httpRequest should matchPattern { case PublicGet("/markets", _) => }
  }

  test("the response is converted to poloniex symbol infos") {
    val response = Json.parse(
      s"""
         ${ json(123).toString() }
         """.stripMargin)
    fakeConversion(json(123), symbolInfo())
    getSymbolInfos.convertResponse(response, jsonConverter) shouldEqual Seq(symbolInfo())
  }

}
