package io.liquirium.connect.binance

import io.liquirium.core.helpers.BasicTest
import io.liquirium.util.HmacCalculator
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BinanceAuthenticatorTest extends BasicTest {

  test("it simply calculates the sha256 signature with the secret") {
    BinanceAuthenticator("key", secret = "top secret!").sign("data") shouldEqual
      HmacCalculator.sha256("data", "top secret!")
  }

}
