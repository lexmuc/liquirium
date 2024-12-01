package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.CancelOrderById
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PrivateDelete
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCancelOrderByIdResponse
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}
import play.api.libs.json.JsValue


class PoloniexApiRequestTest_CancelOrderById extends TestWithMocks {

  val jsonConverter: PoloniexJsonConverter = mock(classOf[PoloniexJsonConverter])

  private def cancelOrderById(
    orderId: String = "abc"
  ): CancelOrderById =
    CancelOrderById(
      orderId = orderId
    )

  private def fakeConversion(json: JsValue, response: PoloniexCancelOrderByIdResponse) =
    jsonConverter.convertCancelOrderByIdResponse(json) returns response

  test("the http request is a private DELETE request") {
    cancelOrderById().httpRequest shouldBe a[PrivateDelete]
  }

  test("the http request has the correct path") {
    cancelOrderById(orderId = "ABCDEF").httpRequest.path shouldEqual "/orders/ABCDEF"
  }

  test("the parameters are empty") {
    cancelOrderById().httpRequest.params shouldEqual Seq()
  }

  test("the response is parsed as poloniex cancel order by id response") {
    fakeConversion(json(123), poloniexCancelOrderByIdResponse("id"))
    cancelOrderById().convertResponse(json(123), jsonConverter) shouldEqual poloniexCancelOrderByIdResponse("id")
  }

}
