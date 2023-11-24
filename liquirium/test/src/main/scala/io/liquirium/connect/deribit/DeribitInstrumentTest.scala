package io.liquirium.connect.deribit

import io.liquirium.core.helpers.BasicTest

class DeribitInstrumentTest extends BasicTest {

  def instrument(s: String): DeribitInstrument = DeribitInstrument(s)

  test("an option is recognized as an option and not as a future") {
    instrument("BTC-28MAR19-9000-C").isOption shouldBe true
    instrument("BTC-28MAR19-9000-C").isFuture shouldBe false
  }

  test("a future is recognized as a future and not as an option") {
    instrument("BTC-28MAR19").isOption shouldBe false
    instrument("BTC-28MAR19").isFuture shouldBe true
  }

  test("something that clearly looks not like a future or option is not recognized as any") {
    instrument("BTC").isOption shouldBe false
    instrument("BTC").isFuture shouldBe false
  }

}
