package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseCreateOrderResponse.{Failure, Success}
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.{JsValue, Json}

class CoinbaseJsonConverterTest_CreateOrderResponse extends BasicTest {

  private def successJson(
    success: Boolean = true,
    orderId: String = "123",
    clientOrderId: String = "ABC-DEF",
  ) = Json.obj(
    "success" -> success,
    "success_response" -> Json.obj(
      "order_id" -> s"$orderId",
      "client_order_id" -> s"$clientOrderId"
    ),
  )

  private def failureJson(
    success: Boolean = false,
    failureReason: String = "FAILURE_REASON",
    error: String = "ERROR",
    message: String = "Message",
    details: String = "Error details",
    previewFailureReason: String = "Preview failure reason",
    newOrderFailureReason: String = "New order failure reason",
  ) = Json.obj(
    "success" -> success,
    "failure_reason" -> s"$failureReason",
    "error_response" -> Json.obj(
      "error" -> s"$error",
      "message" -> s"$message",
      "error_details" -> s"$details",
      "preview_failure_reason" -> s"$previewFailureReason",
      "new_order_failure_reason" -> s"$newOrderFailureReason",
    ),
  )

  val converter = new CoinbaseJsonConverter()

  private def convert(json: JsValue) = converter.convertCreateOrderResponse(json)


  test("in case of success the order id is returned as a string") {
    convert(successJson(orderId = "abc")) should matchPattern {
      case s: Success if s.orderId == "abc" =>
    }
  }

  test("in case of success the client order id is returned as a string") {
    convert(successJson(clientOrderId = "111")) should matchPattern {
      case s: Success if s.clientOrderId == "111" =>
    }
  }

  test("in case of failure the error is returned as a string") {
    convert(failureJson(error = "e")) should matchPattern {
      case f: Failure if f.error == "e" =>
    }
  }

  test("in case of failure the message is returned as a string") {
    convert(failureJson(message = "m")) should matchPattern {
      case f: Failure if f.message == "m" =>
    }
  }

  test("in case of failure the details is returned as a string") {
    convert(failureJson(details = "d")) should matchPattern {
      case f: Failure if f.details == "d" =>
    }
  }

}
