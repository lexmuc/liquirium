package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.TradeHelpers.trade
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OrderTrackingEventTest extends BasicTest {

  test("a trade event has the timestamp of the trade") {
    OrderTrackingEvent.NewTrade(trade("T1", time=sec(123), orderId = Some("O1"))).timestamp shouldEqual sec(123)
  }

  test("a trade event has the orderId of the trade if there is an orderId") {
    OrderTrackingEvent.NewTrade(trade(id = "T1", orderId = Some("O123"))).orderId shouldEqual "O123"
  }

  test("an exception is thrown when a trade event is to be created for a trade without an orderId") {
    intercept[IllegalArgumentException] {
      OrderTrackingEvent.NewTrade(trade(id = "T1", orderId = None)).orderId
    }
  }

  test("an order creation event has the orderId of the order") {
    OrderTrackingEvent.Creation(sec(123), order(123)).orderId shouldEqual order(123).id
  }

}
