package io.liquirium.core.orderTracking.rules

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.orderTracking.rules.helpers.ConsistencyRuleTest

class AtMostOneCreationTest extends ConsistencyRuleTest {

  def rule: ConsistencyRule = AtMostOneCreation

  test("the check passes when no creation or only one creation has been observed") {
    observe(
      change(sec(1), o(10, of = 10)),
    )
    assertPassed()
    observe(
      creation(sec(2), o(10, of = 10)),
    )
    assertPassed()
  }

  test("the check fails with inconsistent events if there are two creations even if they are equal") {
    observe(
      change(sec(1), o(10, of = 10)),
      creation(sec(2), o(10, of = 10)),
      creation(sec(2), o(10, of = 10)),
    )
    assertInconsistentEvents(
      creation(sec(2), o(10, of = 10)),
      creation(sec(2), o(10, of = 10)),
    )
  }

}
