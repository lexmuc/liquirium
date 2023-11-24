package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.GetPairInfos
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.pairInfo
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsValue, Json}

class BitfinexRestApiTest_ConfigsPairInfo extends TestWithMocks {

  val jsonConverter: BitfinexJsonConverter = mock[BitfinexJsonConverter]

  private def getInfos = GetPairInfos()

  private def fakeConversion(json: JsValue, symbolInfos: BitfinexPairInfo*) =
    jsonConverter.convertPairInfos(json) returns symbolInfos.toSeq

  test("requesting symbol infos yields a get request with the proper url") {
    getInfos.httpRequest(jsonConverter) should matchPattern {
      case PublicBitfinexGetRequest("conf/pub:info:pair", _) =>
    }
  }

  test("the response is converted to bitfinex pair infos") {
    val response = Json.parse(
      s"""
         |[
         |${ json(123).toString() }
         |]
         """.stripMargin)
    fakeConversion(json(123), pairInfo(123))
    getInfos.convertResponse(response, jsonConverter) shouldEqual Seq(pairInfo(123))
  }

}
