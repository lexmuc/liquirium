package io.liquirium.bot

import io.liquirium.bot.OrderMatcher.TolerantMatcher
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.Order
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.OperationIntentHelpers.{orderIntent => oi}
import io.liquirium.core.helpers.OrderHelpers.basicOrderData
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OrderMatcherTest extends BasicTest {

  private def o(priceAndQuantity: (BigDecimal, BigDecimal)) = basicOrderData("X", priceAndQuantity)

  private def exactMatch(od: Order.BasicOrderData, i: OrderIntent) = OrderMatcher.ExactMatcher(od, i)

  private def tolerantMatch(
    priceTolerance: Double = 1.0,
    quantityTolerance: Double = 1.0
  )(
    od: Order.BasicOrderData,
    i: OrderIntent
  ) = TolerantMatcher(priceTolerance = priceTolerance, quantityTolerance = quantityTolerance)(od, i)

  test("an exact matcher matches orders only when price and quantity are exactly equal") {
    exactMatch(o(dec("1.2") -> dec("3.4")), oi(dec("1.2") -> dec("3.4"))) shouldBe true
    exactMatch(o(dec("1.2") -> dec("3.4")), oi(dec("1.2") -> dec("3.5"))) shouldBe false
    exactMatch(o(dec("1.2") -> dec("3.4")), oi(dec("1.1") -> dec("3.4"))) shouldBe false
  }

  test("a tolerant matcher tolerates price deviation up to the given factor") {
    tolerantMatch(priceTolerance = 2.0)(o(dec("2.0") -> dec("3.4")), oi(dec("1.0") -> dec("3.4"))) shouldBe true
    tolerantMatch(priceTolerance = 2.0)(o(dec("2.0") -> dec("3.4")), oi(dec("4.0") -> dec("3.4"))) shouldBe true
    tolerantMatch(priceTolerance = 2.0)(o(dec("2.0") -> dec("3.4")), oi(dec("0.99") -> dec("3.4"))) shouldBe false
    tolerantMatch(priceTolerance = 2.0)(o(dec("2.0") -> dec("3.4")), oi(dec("4.01") -> dec("3.4"))) shouldBe false
  }

  test("a tolerant matcher tolerates quantity deviation up to the given factor") {
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("2.0")), oi(dec("1.2") -> dec("1.0"))) shouldBe true
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("2.0")), oi(dec("1.2") -> dec("4.0"))) shouldBe true
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("2.0")), oi(dec("1.2") -> dec("0.99"))) shouldBe false
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("2.0")), oi(dec("1.2") -> dec("4.01"))) shouldBe false
  }

  test("the quantity deviation is properly applied to negative prices") {
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("-2.0")), oi(dec("1.2") -> dec("-1.0"))) shouldBe true
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("-2.0")), oi(dec("1.2") -> dec("-4.0"))) shouldBe true
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("-2.0")), oi(dec("1.2") -> dec("-0.99"))) shouldBe false
    tolerantMatch(quantityTolerance = 2.0)(o(dec("1.2") -> dec("-2.0")), oi(dec("1.2") -> dec("-4.01"))) shouldBe false
  }

  test("a tolerant matcher tolerates a combination of price and quantity deviation") {
    tolerantMatch(priceTolerance = 2.0, quantityTolerance = 2.0)(
      o(dec("1.2") -> dec("3.4")), oi(dec("1.23") -> dec("3.45"))
    ) shouldBe true
  }

}
