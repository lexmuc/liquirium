package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.TradeHelpers.trade

class OrderTrackingEventTest extends BasicTest {

  test("a trade event has the timestamp of the trade") {
    OrderTrackingEvent.NewTrade(trade(time=sec(123))).timestamp shouldEqual sec(123)
  }

  test("an order creation event has the orderId of the order") {
    OrderTrackingEvent.Creation(sec(123), order(123)).orderId shouldEqual order(123).id
  }

}
