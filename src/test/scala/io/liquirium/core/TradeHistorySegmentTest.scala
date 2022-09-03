package io.liquirium.core

import io.liquirium.core.helpers.BasicTest

import java.time.Instant

class TradeHistorySegmentTest extends BasicTest {

  protected def fromForwardTrades(start: Instant)(tt: Trade*): TradeHistorySegment =
    TradeHistorySegment.fromForwardTrades(start, tt)

  protected def empty(start: Instant): TradeHistorySegment = TradeHistorySegment.empty(start)

}
