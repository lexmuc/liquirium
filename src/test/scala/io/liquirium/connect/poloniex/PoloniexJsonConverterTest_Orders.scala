package io.liquirium.connect.poloniex

import io.liquirium.core.Side
import io.liquirium.core.helpers.BasicTest
import play.api.libs.json.Json

import java.time.Instant

//noinspection RedundantDefaultArgument
class PoloniexJsonConverterTest_Orders extends BasicTest {

  private def orderJson(
    id: String = "12345",
    clientOrderId: String = "Try1",
    symbol: String = "TRX_USDC",
    accountType: String = "SPOT",
    side: String = "SELL",
    `type`: String = "LIMIT_MAKER",
    timeInForce: String = "GTC",
    price: String = "100",
    avgPrice: String = "99",
    quantity: String = "5",
    amount: String = "500",
    filledQuantity: String = "10",
    filledAmount: String = "5",
    state: String = "FILLED",
    orderSource: String = "API",
    createTime: Long = 1000,
    updateTime: Long = 500,
  ) =
    s"""{
       |"id":"$id",
       |"clientOrderId":"$clientOrderId",
       |"symbol":"$symbol",
       |"accountType":"$accountType",
       |"side":"$side",
       |"type":"${ `type` }",
       |"timeInForce":"$timeInForce",
       |"price":"$price",
       |"avgPrice":"$avgPrice",
       |"quantity":"$quantity",
       |"amount":"$amount",
       |"filledQuantity":"$filledQuantity",
       |"filledAmount":"$filledAmount",
       |"state":"$state",
       |"orderSource":"$orderSource",
       |"createTime":$createTime,
       |"updateTime":$updateTime
       |}""".stripMargin

  val converter = new PoloniexJsonConverter()

  private def convert(s: String) = converter.convertOrder(Json.parse(s))

  private def convertMany(s: String) = converter.convertOrders(Json.parse(s))


  test("orders are returned with correct id as a string") {
    convert(orderJson(id = "123")).id shouldEqual "123"
  }

  test("the order clientOrderId is returned as a string") {
    convert(orderJson(clientOrderId = "COID")).clientOrderId shouldEqual "COID"
  }

  test("orders are returned with the correct symbol") {
    convert(orderJson(symbol = "TRX_USDC")).symbol shouldEqual "TRX_USDC"
  }

  test("orders are returned with the correct state") {
    convert(orderJson(state = "CANCELED")).state shouldEqual "CANCELED"
  }

  test("orders are returned with the correct accountType") {
    convert(orderJson(accountType = "SPOT")).accountType shouldEqual "SPOT"
  }

  test("orders are returned with the correct side") {
    convert(orderJson(side = "SELL")).side shouldEqual Side.Sell
  }

  test("the order type is returned as a string") {
    convert(orderJson(`type` = "someType")).`type` shouldEqual "someType"
  }

  test("orders are returned with the correct timeInForce") {
    convert(orderJson(timeInForce = "FOK")).timeInForce shouldEqual "FOK"
  }

  test("order price, average price, quantity and amount are returned as a decimal") {
    val o = convert(orderJson(
      price = "2.34",
      avgPrice = "1.98",
      quantity = "3.21",
      amount = "1.23",
    ))
    o.price shouldEqual BigDecimal("2.34")
    o.avgPrice shouldEqual BigDecimal("1.98")
    o.quantity shouldEqual BigDecimal("3.21")
    o.amount shouldEqual BigDecimal("1.23")
  }

  test("filledQuantity and filledAmount are returned as a decimal") {
    val o = convert(orderJson(
      filledQuantity = "2.34",
      filledAmount = "1.98",
    ))
    o.filledQuantity shouldEqual BigDecimal("2.34")
    o.filledAmount shouldEqual BigDecimal("1.98")
  }

  test("orders are returned with the correct createTime and updateTime") {
    val o = convert(orderJson(
      createTime = 3600,
      updateTime = 1200,
    ))
    o.createTime shouldEqual Instant.ofEpochMilli(3600)
    o.updateTime shouldEqual Instant.ofEpochMilli(1200)
  }

  test("several orders can be parsed at once") {
    convertMany("[" + orderJson(id = "1") + "," + orderJson(id = "2") + "]")
      .map(_.id) shouldEqual Seq("1", "2")
  }

}
