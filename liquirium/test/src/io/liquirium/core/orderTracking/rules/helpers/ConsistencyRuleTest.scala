package io.liquirium.core.orderTracking.rules.helpers

import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents
import io.liquirium.core.orderTracking.OrderTrackingEvent
import io.liquirium.core.orderTracking.helpers.SingleOrderTrackingTest
import io.liquirium.core.orderTracking.rules.ConsistencyRule
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

abstract class ConsistencyRuleTest extends SingleOrderTrackingTest {

  def rule: ConsistencyRule

  def assertPassed(): Unit = {
    rule.check(basicState) shouldBe None
  }

  def assertInconsistentEvents(e1: OrderTrackingEvent, e2: OrderTrackingEvent): Unit = {
    rule.check(basicState) shouldBe Some(InconsistentEvents(e1, e2))
  }

}
