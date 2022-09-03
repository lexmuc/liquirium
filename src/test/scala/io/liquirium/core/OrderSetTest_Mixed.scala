package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.OrderHelpers.{order => o}

class OrderSetTest_Mixed extends BasicTest {

  def os(oo: Order*): OrderSet = OrderSet(oo.toSet)

  test("orders can be added and it contains all orders added to it") {
    (OrderSet.empty + o(1) + o(2)) should contain theSameElementsAs Set(o(1), o(2))
  }

  test("orders can be removed") {
    (os(o(1), o(2)) - o(2)) shouldEqual os(o(1))
  }

  test("orders can be removed by id") {
    os(o("A"), o("B")).removeId("B") shouldEqual os(o("A"))
  }

  test("it can be created from a set of orders") {
    (OrderSet.empty + o(1) + o(2)) shouldEqual OrderSet(Set(o(1), o(2)))
  }

  test("it can be created with orders in varargs") {
    OrderSet(o(1), o(2)) shouldEqual OrderSet(Set(o(1), o(2)))
  }

  test("the iterator yields all elements") {
    os(o(1), o(2), o(3)).iterator.toSet shouldEqual Set(o(1), o(2), o(3))
  }

  // #TODO OPTIMIZATION
//  test("buy orders are exposed as sorted sequence") {
//    os(buy(2, at = 5), sell(10, at = 2), buy(5, at = 2), buy(1, at = 10))
//      .sortedBuyOrders should equal(Seq(buy(1, at = 10), buy(2, at = 5), buy(5, at = 2)))
//  }
//
//  test("Sell orders are exposed as sorted sequence") {
//    os(sell(3, at = 8), buy(5, at = 2), sell(10, at = 2), sell(5, at = 4))
//      .sortedSellOrders should equal(Seq(sell(10, at = 2), sell(5, at = 4), sell(3, at = 8)))
//  }
//
//  test("it can be tested whether an order is contained in the set") {
//    val set = os(sellOrder("x"), buyOrder("y"))
//    set.contains(buyOrder("y")) shouldBe true
//    set.contains(sellOrder("x")) shouldBe true
//    set.contains(buyOrder("z")) shouldBe false
//    set.contains(sellOrder("z")) shouldBe false
//  }
//
//  test("it can be tested whether an order id is contained in the set") {
//    val set = os(sellOrder("x"), buyOrder("y"))
//    set.containsId("x") shouldBe true
//    set.containsId("y") shouldBe true
//    set.containsId("z") shouldBe false
//  }
//
//  test("buy orders are inserted between lower and higher orders") {
//    (os(buy(5, at = 2), buy(1, at = 10)) + buy(2, at = 5))
//      .sortedBuyOrders should equal(Seq(buy(1, at = 10), buy(2, at = 5), buy(5, at = 2)))
//  }
//
//  test("buy orders are inserted before other orders at the same price") {
//    (os(buy(5, at = 2)) + buy(4, at = 2)).sortedBuyOrders shouldEqual Seq(buy(4, at = 2), buy(5, at = 2))
//  }
//
//  test("sell orders are inserted between lower and higher orders") {
//    (os(sell(3, at = 8), sell(10, at = 2)) + sell(5, at = 4))
//      .sortedSellOrders shouldEqual Seq(sell(10, at = 2), sell(5, at = 4), sell(3, at = 8))
//  }
//
//  test("sell orders are inserted before other orders at the same price") {
//    (os(sell(5, at = 2)) + sell(4, at = 2)).sortedSellOrders shouldEqual Seq(sell(4, at = 2), sell(5, at = 2))
//  }

}
