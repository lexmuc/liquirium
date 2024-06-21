package io.liquirium.core.orderTracking.rules

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.orderTracking.rules.helpers.ConsistencyRuleTest

class FullQuantityAtCreationMatchesTheLatestPresentObservationTest extends ConsistencyRuleTest {

  def rule: ConsistencyRule = FullQuantityAtCreationMatchesTheLatestPresentObservation

  test("the check passes when no creation or observation has occurred") {
    observe(
      disappearance(sec(0)),
      trade(sec(1), 10),
    )
    assertPassed()
  }

  test("the check passes when no creation has been observed") {
    observe(
      change(sec(1), o(10, of = 10)),
    )
    assertPassed()
  }

  test("the check passes if only a creation has been observed") {
    observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
    )
    assertPassed()
  }

  test("the check passes if the creation matches a single observation") {
    observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(10, of = 10)),
    )
    assertPassed()
  }

  test("the check fails if the creation does not match a single observation") {
    observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(9, of = 9)),
    )
    assertInconsistentEvents(
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(9, of = 9)),
    )
  }

  test("the check passes if the latest present observation is matched but not an earlier observation") {
    observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(8, of = 8)),
      change(sec(3), o(10, of = 10)),
    )
    assertPassed()
  }

}
