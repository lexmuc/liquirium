package io.liquirium.bot

import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.Order.BasicOrderData
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.OperationIntentHelpers.{cancelIntent => ci, convenientOrderIntent => oi}
import io.liquirium.core.helpers.OrderHelpers.TestBasicOrderData
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class SimpleOrderIntentSyncerTest extends BasicTest {

  var orderMatcher: OrderMatcher = OrderMatcher.ExactMatcher

  private def sync(intents: OrderIntent*)(orders: BasicOrderData*) =
    SimpleOrderIntentSyncer(orderMatcher).apply(intents, orders.toSet)

  private def o(id: String, priceAndQuantity: (Int, Int)) =
    TestBasicOrderData(id, dec(priceAndQuantity._2), price = dec(priceAndQuantity._1))

  test("it cancels all orders when there are no intents") {
    sync()(o("A", 1 -> 2), o("B", 3 -> -2)) should contain theSameElementsAs Seq(ci("A"), ci("B"))
  }

  test("it cancels all buy orders when a single buy intent is not matched by an order") {
    sync(oi(1 -> 2), oi(2 -> 2))(o("B1", 1 -> 2), o("B2", 2 -> 3)) should contain theSameElementsAs
      Seq(ci("B1"), ci("B2"))
  }

  test("it cancels all sell orders when a single sell intent is not matched by an order") {
    sync(oi(1 -> -2), oi(2 -> -2))(o("S1", 1 -> -2), o("S2", 2 -> -3)) should contain theSameElementsAs
      Seq(ci("S1"), ci("S2"))
  }

  test("it cancels all buy orders when there is an extra order not matched by an intent") {
    sync(oi(1 -> 2), oi(2 -> 2))(o("B1", 1 -> 2), o("B2", 2 -> 2), o("B3", 3 -> 3)) should contain theSameElementsAs
      Seq(ci("B1"), ci("B2"), ci("B3"))
  }

  test("it cancels all sell orders when there is an extra order not matched by an intent") {
    sync(oi(1 -> -2), oi(2 -> -2))(o("S1", 1 -> -2), o("S2", 2 -> -2), o("S3", 3 -> -3)) should
      contain theSameElementsAs Seq(ci("S1"), ci("S2"), ci("S3"))
  }

  test("it cancels all buy orders when a single buy intent is matched by more than one order") {
    sync(oi(1 -> 2), oi(2 -> 2))(o("B1A", 1 -> 2), o("B1B", 1 -> 2), o("B2", 2 -> 3)) should contain theSameElementsAs
      Seq(ci("B1A"), ci("B1B"), ci("B2"))
  }

  test("it cancels all sell orders when a single sell intent is matched by more than one order") {
    sync(oi(1 -> -2), oi(2 -> -3))(o("S1A", 1 -> -2), o("S1B", 1 -> -2), o("S2", 2 -> -3)) should
      contain theSameElementsAs Seq(ci("S1A"), ci("S1B"), ci("S2"))
  }

  test("it returns nothing when the orders match the intents") {
    sync(oi(1 -> 2), oi(3 -> -2), oi(2 -> 2))(o("A", 1 -> 2), o("B", 3 -> -2), o("C", 2 -> 2)) shouldEqual Seq()
    sync(oi(1 -> 2), oi(3 -> -2), oi(4 -> -2))(o("A", 1 -> 2), o("B", 3 -> -2), o("C", 4 -> -2)) shouldEqual Seq()
  }

  test("matching sells are not affected when buys are changed") {
    sync(oi(1 -> 2), oi(2 -> -3))(o("B", 1 -> 3), o("S", 2 -> -3)) shouldEqual Seq(ci("B"))
  }

  test("matching buys are not affected when sells are changed") {
    sync(oi(1 -> 2), oi(2 -> -3))(o("B", 1 -> 2), o("S", 2 -> -2)) shouldEqual Seq(ci("S"))
  }

  test("several orders of the same quantity at the same price yield no cancels when they are matched") {
    sync(
      oi(1 -> 2),
      oi(1 -> 2),
      oi(2 -> -3),
      oi(2 -> -3),
    )(
      o("B1", 1 -> 2),
      o("B2", 1 -> 2),
      o("S1", 2 -> -3),
      o("S2", 2 -> -3),
    ) shouldEqual Seq()
  }

  test("identical orders of the same quantity and price are cancelled when they do not match the intended count") {
    sync(
      oi(1 -> 2),
      oi(1 -> 2),
      oi(2 -> -3),
      oi(2 -> -3),
    )(
      o("B1", 1 -> 2),
      o("S1", 2 -> -3),
      o("S2", 2 -> -3),
      o("S3", 2 -> -3),
    ) should contain theSameElementsAs Seq(
      ci("B1"),
      ci("S1"),
      ci("S2"),
      ci("S3"),
    )
  }

  test("all order intents are forwarded when there are no orders") {
    sync(oi(1 -> 2), oi(3 -> -2), oi(2 -> 2))() should contain theSameElementsAs
      Seq(oi(1 -> 2), oi(3 -> -2), oi(2 -> 2))
  }

  test("the given order matcher is used when matching orders and intents") {
    orderMatcher = new OrderMatcher {
      override def apply(order: BasicOrderData, intent: OrderIntent): Boolean = true
    }
    sync(oi(1 -> 2), oi(3 -> -2))(o("A", 2 -> 2), o("B", 3 -> -3)) shouldEqual Seq()
  }

}
