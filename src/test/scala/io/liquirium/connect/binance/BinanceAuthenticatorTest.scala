package io.liquirium.connect.binance

import io.liquirium.core.helper.BasicTest
import io.liquirium.util.HmacCalculator

class BinanceAuthenticatorTest extends BasicTest {

  test("it simply calculates the sha256 signature with the secret") {
    BinanceAuthenticator("key", secret = "top secret!").sign("data") shouldEqual
      HmacCalculator.sha256("data", "top secret!")
  }

}
