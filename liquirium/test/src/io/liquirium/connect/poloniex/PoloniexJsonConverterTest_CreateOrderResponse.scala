package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsObject, JsString, JsValue}

class PoloniexJsonConverterTest_CreateOrderResponse extends BasicTest {

  private def convertResponse(json: JsValue) = new PoloniexJsonConverter().convertCreateOrderResponse(json)

  private def responseJson(
    id: String = "abc",
    clientOrderId: String = "xxx",
  ) =
    JsObject(Seq(
      "id" -> JsString(id),
      "clientOrderId" -> JsString(clientOrderId)
    ))

  test("it parses the id field") {
    val r = responseJson(id = "xyz")
    convertResponse(r).id shouldEqual "xyz"
  }

  test("it parses the client order id field") {
    val r = responseJson(clientOrderId = "xyz")
    convertResponse(r).clientOrderId shouldEqual "xyz"
  }

}
