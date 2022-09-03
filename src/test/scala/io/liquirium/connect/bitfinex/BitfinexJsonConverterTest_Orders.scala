package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexOrder.{OrderStatus, OrderType}
import io.liquirium.core.helpers.BasicTest
import play.api.libs.json.{JsArray, JsValue, Json}

import java.time.Instant

class BitfinexJsonConverterTest_Orders extends BasicTest {

  private def convert(json: JsValue) = new BitfinexJsonConverter().convertSingleOrder(json)

  private def convertSeveral(jj: JsValue*) = new BitfinexJsonConverter().convertOrders(JsArray(jj.toSeq))

  private def json(
    orderId: Long = 0,
    clientOrderId: Long = 0,
    symbol: String = "abcdef",
    creationMillis: Long = 12345,
    updateMillis: Long = 12345,
    amount: BigDecimal = BigDecimal("0"),
    originalAmount: BigDecimal = BigDecimal("0"),
    `type`: String = "LIMIT",
    status: String = "ACTIVE",
    price: BigDecimal = BigDecimal("0"),
  ) = Json.parse(
    s"""[
       |$orderId,
       |null,
       |$clientOrderId,
       |"$symbol",
       |$creationMillis,
       |$updateMillis,
       |${ amount.toString },
       |${ originalAmount.toString },
       |"${ `type` }",
       |null,
       |null,
       |null,
       |0,
       |"$status",
       |null,
       |null,
       |${ price.toString },
       |0,0,0,null,null,null,0,0,null,null,null,"BFX",null,null,null
       |]""".stripMargin
  )

  test("the id is extracted") {
    convert(json(orderId = 123)).id shouldBe 123
  }

  test("the client order id is extracted") {
    convert(json(clientOrderId = 222)).clientOrderId shouldBe 222
  }

  test("the symbol is extracted") {
    convert(json(symbol = "tABCDEF")).symbol shouldEqual "tABCDEF"
  }

  test("the creation and update timestamps are interpreted as milliseconds") {
    val o = convert(json(creationMillis = 66666, updateMillis = 77777))
    o.creationTimestamp shouldEqual Instant.ofEpochMilli(66666)
    o.updateTimestamp shouldEqual Instant.ofEpochMilli(77777)
  }

  test("amount and original amount are extracted") {
    val o = convert(json(amount = 1.23, originalAmount = 2.34))
    o.amount shouldEqual BigDecimal("1.23")
    o.originalAmount shouldEqual BigDecimal("2.34")
  }

  test("only the supported types are extracted") {
    convert(json(`type` = "LIMIT")).`type` shouldEqual OrderType.Limit
    convert(json(`type` = "EXCHANGE LIMIT")).`type` shouldEqual OrderType.ExchangeLimit
    convert(json(`type` = "MARKET")).`type` shouldEqual OrderType.Market
    convert(json(`type` = "EXCHANGE MARKET")).`type` shouldEqual OrderType.ExchangeMarket
    an[Exception] shouldBe thrownBy(convert(json(`type` = "X")))
  }

  test("a known order status is properly parsed") {
    convert(json(status = "ACTIVE")).status shouldEqual OrderStatus.Active
    convert(json(status = "PARTIALLY FILLED @ 0.123(-300.007), PARTIALLY FILLED @ 0.124(-100)"))
      .status shouldEqual OrderStatus.PartiallyFilled
    convert(json(status = "EXECUTED @ 0.032926(0.96678435)")).status shouldEqual OrderStatus.Executed
    convert(json(status = "CANCELED was: PARTIALLY FILLED @ 0.014(-1.6368)")).status shouldEqual OrderStatus.Canceled
    convert(json(status = "CANCELED")).status shouldEqual OrderStatus.Canceled
    convert(json(status = "POSTONLY CANCELED")).status shouldEqual OrderStatus.PostOnlyCanceled

    an[Exception] shouldBe thrownBy(convert(json(status = "X")))
  }

  test("the price is extracted") {
    convert(json(price = 3.21)).price shouldEqual BigDecimal("3.21")
  }

  test("it can convert several orders at once") {
    convertSeveral(json(1), json(2)).map(_.id) shouldEqual Seq(1, 2)
  }

}
