package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.OrderHelpers
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{syncedStateWithReportableOrder, syncedStateWithoutReportableOrder}
import io.liquirium.core.Order
import io.liquirium.eval.IncrementalMap
import io.liquirium.eval.helpers.EvalTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OpenOrdersBasedOnTrackingStatesTest extends EvalTest {

  private var mapValue: IncrementalMap[String, BasicOrderTrackingState] = IncrementalMap.empty
  private val inputEval = testEval[IncrementalMap[String, BasicOrderTrackingState]]()

  fakeEvalValue(inputEval, mapValue)

  private def updateState(id: String, state: BasicOrderTrackingState): Unit = {
    mapValue = mapValue.update(id, state)
    fakeEvalValue(inputEval, mapValue)
  }

  private val evalUnderTest = OpenOrdersBasedOnTrackingStates(inputEval)

  private def assertOrderSet(oo: Order*): Unit = {
    evaluate(evalUnderTest).get shouldEqual oo.toSet
  }

  private def assertEmptyOrderSet(): Unit = assertOrderSet()

  test("the order set is empty when there are no tracking states") {
    assertEmptyOrderSet()
  }

  test("the order set is empty when there are only states without reportable order") {
    val o1 = OrderHelpers.order(1)
    val o2 = OrderHelpers.order(1)
    updateState(o1.id, syncedStateWithoutReportableOrder(o1.id))
    assertEmptyOrderSet()
    updateState(o2.id, syncedStateWithoutReportableOrder(o2.id))
    assertEmptyOrderSet()
  }

  test("the order set contains only the reportable orders in the reportable state") {
    val o1 = OrderHelpers.order(1)
    val o2 = OrderHelpers.order(2)
    val o3 = OrderHelpers.order(3)
    updateState(o1.id, syncedStateWithReportableOrder(o1))
    updateState(o2.id, syncedStateWithoutReportableOrder(o2.id))
    updateState(o3.id, syncedStateWithReportableOrder(o3))
    assertOrderSet(o1, o3)
  }

}
