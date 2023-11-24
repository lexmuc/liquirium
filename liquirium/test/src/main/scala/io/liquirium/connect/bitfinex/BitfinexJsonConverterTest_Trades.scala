package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import play.api.libs.json.{JsArray, JsValue, Json}

import java.time.Instant

class BitfinexJsonConverterTest_Trades extends BasicTest {

  private def convert(json: JsValue) = new BitfinexJsonConverter().convertSingleTrade(json)

  private def convertSeveral(jj: JsValue*) = new BitfinexJsonConverter().convertTrades(JsArray(jj.toSeq))

  private def json(
    tradeId: Long = 0,
    pair: String = "abcdef",
    timestamp: Long = 0,
    orderId: Long = 0,
    amount: BigDecimal = dec("0"),
    price: BigDecimal = dec("0"),
    fee: BigDecimal = dec("0"),
    feeCurrency: String = "",
  ) = Json.parse(
    s"""[
       |$tradeId,
       |"$pair",
       |$timestamp,
       |$orderId,
       |${ amount.toString },
       |${ price.toString },
       |null,
       |null,
       |null,
       |${ fee.toString },
       |"$feeCurrency"
       |]""".stripMargin
  )

  test("it extracts the id") {
    convert(json(tradeId = 1234)).id shouldBe 1234
  }

  test("it extracts the pair") {
    convert(json(pair = "tASDJKL")).symbol shouldEqual "tASDJKL"
  }

  test("it extracts the timestamp") {
    convert(json(timestamp = 12345)).timestamp shouldBe Instant.ofEpochMilli(12345)
  }

  test("it extracts the order id") {
    convert(json(orderId = 4444)).orderId shouldBe 4444
  }

  test("it extracts the amount") {
    convert(json(amount = 1.23)).amount shouldEqual dec("1.23")
  }

  test("it extracts the price") {
    convert(json(price = 0.12)).price shouldEqual dec("0.12")
  }

  test("it extracts the fee") {
    convert(json(fee = 1.11)).fee shouldEqual dec("1.11")
  }

  test("it extracts the fee currency") {
    convert(json(feeCurrency = "BTC")).feeCurrency shouldEqual "BTC"
  }

  test("it can convert several trades at once") {
    convertSeveral(json(1), json(2)).map(_.id) shouldEqual Seq(1, 2)
  }

}
