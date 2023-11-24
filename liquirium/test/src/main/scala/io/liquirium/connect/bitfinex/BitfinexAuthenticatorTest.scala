package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.util.HmacCalculator


class BitfinexAuthenticatorTest extends BasicTest {

  test("it simply calculates the sha384 signature with the secret") {
    BitfinexAuthenticator("key", secret = "top secret!").sign("data") shouldEqual
      HmacCalculator.sha384("data", "top secret!")
  }

}
