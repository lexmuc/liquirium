package io.liquirium.util

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.milli
import io.liquirium.helpers.FakeClock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class MillisecondNonceGeneratorTest extends BasicTest {

  val clock = new FakeClock(milli(0))

  val gen = new MillisecondNonceGenerator(clock)

  test("it returns the current millisecond") {
    clock.set(milli(123))
    gen.next() shouldEqual 123
    clock.set(milli(124))
    gen.next() shouldEqual 124
  }

  test("it returns at least one more than the last nonce") {
    clock.set(milli(123))
    gen.next() shouldEqual 123
    gen.next() shouldEqual 124
    clock.set(milli(124))
    gen.next() shouldEqual 125
  }

}
