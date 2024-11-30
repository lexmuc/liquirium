package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

class PoloniexJsonConverterTest_CancelOrderByIdResponse extends BasicTest {

  private def convertResponse(json: JsValue) = new PoloniexJsonConverter().convertCancelOrderByIdResponse(json)

  private def responseJson(
    orderId: String = "abc",
    clientOrderId: String = "xxx",
    state: String = "PENDING_CANCEL",
    code: Int = 200,
    message: String = "message"
  ) =
    JsObject(Seq(
      "orderId" -> JsString(orderId),
      "clientOrderId" -> JsString(clientOrderId),
      "state" -> JsString(state),
      "code" -> JsNumber(code),
      "message" -> JsString(message),
    ))

  test("it parses the order id field") {
    val r = responseJson(orderId = "xyz")
    convertResponse(r).orderId shouldEqual "xyz"
  }

  test("it parses the client order id field") {
    val r = responseJson(clientOrderId = "123")
    convertResponse(r).clientOrderId shouldEqual "123"
  }

  test("it parses the state field") {
    val r = responseJson(state = "state")
    convertResponse(r).state shouldEqual "state"
  }

  test("it parses the code field") {
    val r = responseJson(code = 400)
    convertResponse(r).code shouldEqual 400
  }

  test("it parses the message field") {
    val r = responseJson(message = "m")
    convertResponse(r).message shouldEqual "m"
  }

}
