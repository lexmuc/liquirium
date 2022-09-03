package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.CancelOrders
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivatePost
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseCancelOrderResult => ccor}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

class CoinbaseApiRequestTest_CancelOrders extends TestWithMocks {

  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  private def fakeSeqConversion(json: JsValue, results: CoinbaseCancelOrderResult*) =
    jsonConverter.convertCancelOrderResults(json) returns results.toSeq

  private def cancelOrder(
    orderIds: Seq[String] = Seq("abc")
  ) = CancelOrders(
    orderIds
  )

  test("the http request is a private POST request") {
    cancelOrder().httpRequest shouldBe a[PrivatePost]
  }

  test("the http request has the correct path") {
    cancelOrder().httpRequest.path shouldEqual "/api/v3/brokerage/orders/batch_cancel"
  }

  test("the order ids are encoded in the body") {
    (cancelOrder(orderIds = Seq("111")).httpRequest.body \ "order_ids").get shouldEqual JsArray(IndexedSeq(JsString("111")))
  }

  test("the results are parsed from the response 'results' field") {
    val response = JsObject(Seq("results" -> json(123)))
    fakeSeqConversion(json(123), ccor(success = true), ccor(success = false))
    cancelOrder().convertResponse(response, jsonConverter) shouldEqual Seq(ccor(success = true), ccor(success = false))
  }

  test("the response is parsed as a sequence of coinbase cancel order results") {
    val response = JsObject(Seq("results" -> json(123)))
    fakeSeqConversion(json(123), ccor(success = true), ccor(success = false))
    cancelOrder().convertResponse(response, jsonConverter) shouldEqual Seq(ccor(success = true), ccor(success = false))
  }

}
