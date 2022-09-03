package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.helpers.DeribitJsonHelpers.{deribitJsonOrder, deribitTradeJson}
import io.liquirium.core.helpers.BasicTest
import play.api.libs.json.{JsArray, JsObject, JsValue}

class DeribitJsonConverterTest_OrderRequestResponse extends BasicTest {

  val converter = new DeribitJsonConverter()

  private def convert(order: JsValue, trades: JsValue*) =
    converter.convertOrderRequestResponse(JsObject(Seq(
      "order" -> order,
      "trades" -> JsArray(trades)
    )))

  test("the order is converted and set as part of the response") {
    convert(deribitJsonOrder("asdf")).order shouldEqual converter.convertOrder(deribitJsonOrder("asdf"))
  }

  test("the trades are converted as well") {
    convert(deribitJsonOrder(""), deribitTradeJson("t1"), deribitTradeJson("t2")).trades shouldEqual Seq(
      converter.convertTrade(deribitTradeJson("t1")),
      converter.convertTrade(deribitTradeJson("t2"))
    )
  }

}
