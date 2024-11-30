package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.helpers.DeribitJsonHelpers.deribitJsonOrder
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.JsValue

class DeribitJsonConverterTest_CancelRequestResponse extends BasicTest {

  val converter = new DeribitJsonConverter()

  private def convert(order: JsValue) = converter.convertCancelRequestResponse(order)

  test("the order is converted and set as part of the response") {
    convert(deribitJsonOrder("asdf")) shouldEqual
      DeribitCancelRequestResponse(converter.convertOrder(deribitJsonOrder("asdf")))
  }

}
