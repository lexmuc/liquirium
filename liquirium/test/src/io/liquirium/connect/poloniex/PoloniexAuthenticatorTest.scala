package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.helpers.FakeClock
import io.liquirium.util.{ApiCredentials, HmacCalculator}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant


class PoloniexAuthenticatorTest extends BasicTest {

  private var credentials = ApiCredentials(apiKey = "key", secret = "secret")
  private val clock = FakeClock(Instant.ofEpochMilli(0))

  private def authenticator = new PoloniexAuthenticator(credentials, clock)

  test("it generates the correct GET headers") {
    credentials = ApiCredentials(apiKey = "key", secret = "top secret!")
    clock.set(Instant.ofEpochMilli(1000))

    val path = "/orders"
    val params = Seq(("symbol", "BTC_USDT"), ("side", "BUY"))
    val data = "GET\n" + "/orders\n" + "side=BUY&signTimestamp=1000&symbol=BTC_USDT"

    authenticator.getHeadersForGetRequest(path, params) shouldEqual
      Map(
        "key" -> "key",
        "signatureMethod" -> "hmacSHA256",
        "signatureVersion" -> "1",
        "signTimestamp" -> "1000",
        "signature" -> HmacCalculator.sha256Base64(data, credentials.secret),
      )
  }

  test("it generates the correct POST headers without request body") {
    credentials = ApiCredentials(apiKey = "key", secret = "top secret!")
    clock.set(Instant.ofEpochMilli(1000))

    val path = "/wallets/address"
    val body = ""
    val data = "POST\n" + "/wallets/address\n" + "signTimestamp=1000"

    authenticator.getHeadersForPostRequest(path, body) shouldEqual
      Map(
        "key" -> "key",
        "signatureMethod" -> "hmacSHA256",
        "signatureVersion" -> "1",
        "signTimestamp" -> "1000",
        "signature" -> HmacCalculator.sha256Base64(data, credentials.secret),
      )
  }

  test("it generates the correct POST headers with request body") {
    credentials = ApiCredentials(apiKey = "key", secret = "top secret!")
    clock.set(Instant.ofEpochMilli(1000))

    val path = "/wallets/address"
    val body = """{"currency":"TRX"}"""
    val data = "POST\n" + "/wallets/address\n" + "requestBody={\"currency\":\"TRX\"}&signTimestamp=1000"

    authenticator.getHeadersForPostRequest(path, body) shouldEqual
      Map(
        "key" -> "key",
        "signatureMethod" -> "hmacSHA256",
        "signatureVersion" -> "1",
        "signTimestamp" -> "1000",
        "signature" -> HmacCalculator.sha256Base64(data, credentials.secret),
      )
  }

  test("it generates the correct DELETE headers without request body") {
    credentials = ApiCredentials(apiKey = "key", secret = "top secret!")
    clock.set(Instant.ofEpochMilli(1000))

    val path = "/wallets/address"
    val body = ""
    val data = "DELETE\n" + "/wallets/address\n" + "signTimestamp=1000"

    authenticator.getHeadersForDeleteRequest(path, body) shouldEqual
      Map(
        "key" -> "key",
        "signatureMethod" -> "hmacSHA256",
        "signatureVersion" -> "1",
        "signTimestamp" -> "1000",
        "signature" -> HmacCalculator.sha256Base64(data, credentials.secret),
      )
  }

  test("it generates the correct DELETE headers with request body") {
    credentials = ApiCredentials(apiKey = "key", secret = "top secret!")
    clock.set(Instant.ofEpochMilli(1000))

    val path = "/wallets/address"
    val body = """{"currency":"TRX"}"""
    val data = "DELETE\n" + "/wallets/address\n" + "requestBody={\"currency\":\"TRX\"}&signTimestamp=1000"

    authenticator.getHeadersForDeleteRequest(path, body) shouldEqual
      Map(
        "key" -> "key",
        "signatureMethod" -> "hmacSHA256",
        "signatureVersion" -> "1",
        "signTimestamp" -> "1000",
        "signature" -> HmacCalculator.sha256Base64(data, credentials.secret),
      )
  }

}
