package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.OrderHelpers
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers._
import io.liquirium.eval.IncrementalMap
import io.liquirium.eval.helpers.EvalTest

import java.time.{Duration, Instant}

class IsInSyncEvalTest extends EvalTest {

  private var mapValue: IncrementalMap[String, BasicOrderTrackingState] = IncrementalMap.empty
  private val mapInputEval = testEval[IncrementalMap[String, BasicOrderTrackingState]]()
  private val maxSyncDuration = testEval[Duration]()
  private val currentTime = testEval[Instant]()

  private def fakeMaxSyncDuration(d: Duration): Unit = {
    fakeEvalValue(maxSyncDuration, d)
  }

  private def fakeTime(t: Instant): Unit = {
    fakeEvalValue(currentTime, t)
  }

  fakeEvalValue(mapInputEval, mapValue)
  fakeMaxSyncDuration(secs(1))
  fakeTime(sec(0))

  private def updateState(id: String, state: BasicOrderTrackingState): Unit = {
    mapValue = mapValue.update(id, state)
    fakeEvalValue(mapInputEval, mapValue)
  }

  private def o(n: Int) = OrderHelpers.order(n)

  private val evalUnderTest = IsInSyncEval(mapInputEval, maxSyncDuration, currentTime)

  private def assertSync(isInSync: Boolean): Unit = {
    evaluate(evalUnderTest).get shouldBe isInSync
  }

  test("it is in sync when there are no tracking states") {
    assertSync(true)
  }

  test("it is in sync when all orders are in sync") {
    updateState(o(1).id, syncedStateWithReportableOrder(o(1)))
    updateState(o(2).id, syncedStateWithoutReportableOrder(o(2).id))
    assertSync(true)
  }

  test("it is not in sync when there is a single recent order of which we don't know why it is gone") {
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(10))
    updateState(o(2).id, stateWithSyncReasonUnknownWhyGone(o(2), sec(5)))
    assertSync(false)
  }

  test("it is not in sync when there is an order with an error") {
    fakeMaxSyncDuration(secs(30))
    updateState(o(2).id, stateWithReappearingOrderError(o(2), sec(5)))
    fakeTime(sec(10))
    assertSync(false)
    fakeTime(sec(100))
    assertSync(false)
  }

  test("when there is a cancelled order for which trades might still be seen it is syncing for the sync duration") {
    fakeMaxSyncDuration(secs(30))
    updateState(o(1).id, stateWithSyncReasonUnknownIfMoreTrades(o(1), sec(5)))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(true)
  }

  test("if enough time passes we are in sync again even if we don't know why an order is gone (cancel assumed)") {
    updateState(o(1).id, stateWithSyncReasonUnknownWhyGone(o(1), sec(5)))
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(true)
  }

  test("expected trades are not resolved after the given duration") {
    updateState(o(1).id, stateWithSyncReasonExpectingTrades(o(1).id, sec(5), 5))
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(false)
  }

  test("expected order changes are not resolved after the given duration") {
    updateState(o(1).id, stateWithSyncReasonExpectingObservationChange(o(1), sec(5)))
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(false)
  }

  test("the presence of synced orders does not make the state synced as long as there is one order out of sync") {
    updateState(o(1).id, syncedStateWithReportableOrder(o(1)))
    updateState(o(2).id, stateWithSyncReasonExpectingObservationChange(o(2), sec(5)))
    updateState(o(3).id, syncedStateWithReportableOrder(o(3)))
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(false)
  }

  test("the presence of synced orders does not make the state synced as long as there is one order with an error") {
    updateState(o(1).id, syncedStateWithReportableOrder(o(1)))
    updateState(o(2).id, stateWithReappearingOrderError(o(2), sec(5)))
    updateState(o(3).id, syncedStateWithReportableOrder(o(3)))
    fakeMaxSyncDuration(secs(30))
    fakeTime(sec(34))
    assertSync(false)
    fakeTime(sec(35))
    assertSync(false)
  }

}
