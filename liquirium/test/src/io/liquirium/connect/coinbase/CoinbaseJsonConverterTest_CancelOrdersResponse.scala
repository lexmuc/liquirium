package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsValue, Json}

class CoinbaseJsonConverterTest_CancelOrdersResponse extends BasicTest {

  private def resultJson(
    success: Boolean = true,
    failureReason: String = "failure reason",
    orderId: String = "abc"
  ) = Json.obj(
    "success" -> success,
    "failure_reason" -> failureReason,
    "order_id" -> orderId
  )

  val converter = new CoinbaseJsonConverter()

  private def convertResult(json: JsValue) = converter.convertSingleCancelOrderResult(json)

  private def convertResults(json: JsValue) = converter.convertCancelOrderResults(json)

  test("the success field of a result is parsed as boolean") {
    convertResult(resultJson(success = true)).success shouldEqual true
  }

  test("the failure reason of a result is parsed as string") {
    convertResult(resultJson(failureReason = "fr")).failureReason shouldEqual "fr"
  }

  test("the order id of a result is parsed as string") {
    convertResult(resultJson(orderId = "oi")).orderId shouldEqual "oi"
  }

  test("the response is parsed as a sequence of cancel order results") {
    convertResults(JsArray(Seq(resultJson(success = true), resultJson(success = false))))
      .map(_.success) shouldEqual Seq(true, false)
  }

}
