package io.liquirium.util

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class IncrementingNonceGeneratorTest extends BasicTest {

  test("it simply increments the nonce starting with the init value + 1") {
    val gen = new IncrementingNonceGenerator(init = 4711)
    gen.next() shouldEqual 4712
    gen.next() shouldEqual 4713
    gen.next() shouldEqual 4714
  }

}
