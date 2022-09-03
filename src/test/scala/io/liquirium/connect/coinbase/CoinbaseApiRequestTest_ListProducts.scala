package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.ListProducts
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivateGet
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.productInfo
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsValue, Json}

class CoinbaseApiRequestTest_ListProducts extends TestWithMocks {

  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  private def listProducts = ListProducts()

  private def fakeConversion(json: JsValue, symbolInfos: CoinbaseProductInfo*) =
    jsonConverter.convertProducts(json) returns symbolInfos.toSeq

  test("requesting symbol infos yields a get request with the proper url") {
    listProducts.httpRequest should matchPattern { case PrivateGet("/api/v3/brokerage/products", _) => }
  }

  test("the response is converted to coinbase symbol infos") {
    val response = Json.parse(
      s"""{
          "products": ${ json(123).toString() }
         }""".stripMargin)
    fakeConversion(json(123), productInfo())
    listProducts.convertResponse(response, jsonConverter) shouldEqual Seq(productInfo())
  }

}
