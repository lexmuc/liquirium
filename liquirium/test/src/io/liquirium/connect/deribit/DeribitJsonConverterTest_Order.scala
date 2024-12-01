package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.DeribitOrder.State
import io.liquirium.connect.deribit.helpers.DeribitJsonHelpers.deribitJsonOrder
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsValue}

class DeribitJsonConverterTest_Order extends BasicTest {

  private def convert(json: JsValue) = new DeribitJsonConverter().convertOrder(json)

  private def convertMany(json: JsValue*) = new DeribitJsonConverter().convertOrders(JsArray(json))

  test("it extracts the id") {
    convert(deribitJsonOrder(orderId = "4711X")).id shouldEqual "4711X"
  }

  test("it extracts the direction") {
    convert(deribitJsonOrder(direction = "buy")).direction shouldEqual DeribitDirection.Buy
    convert(deribitJsonOrder(direction = "selle")).direction shouldEqual DeribitDirection.Sell
  }

  test("it extracts the price") {
    convert(deribitJsonOrder(price = BigDecimal("23.45"))).price shouldEqual BigDecimal("23.45")
  }

  test("it extracts the quantity/amount") {
    convert(deribitJsonOrder(amount = BigDecimal("1.23"))).quantity shouldEqual BigDecimal("1.23")
  }

  test("it extracts the filled quantity/amount") {
    convert(deribitJsonOrder(filledQuantity = BigDecimal("1.2377"))).filledQuantity shouldEqual BigDecimal("1.2377")
  }

  test("it extracts the instrument") {
    convert(deribitJsonOrder(instrumentName = "inst123")).instrument shouldEqual "inst123"
  }

  test("several orders can be parsed at once") {
    convertMany(deribitJsonOrder("A"), deribitJsonOrder(("B"))).map(_.id) shouldEqual Seq("A", "B")
  }

  test("it extracts the state (all allowed states)") {
    convert(deribitJsonOrder(orderState = "open")).state shouldEqual State.Open
    convert(deribitJsonOrder(orderState = "filled")).state shouldEqual State.Filled
    convert(deribitJsonOrder(orderState = "cancelled")).state shouldEqual State.Cancelled
    convert(deribitJsonOrder(orderState = "untriggered")).state shouldEqual State.Untriggered
    convert(deribitJsonOrder(orderState = "rejected")).state shouldEqual State.Rejected
  }

}
