package io.liquirium.connect.binance

import io.liquirium.core.Side
import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.dec
import play.api.libs.json.{JsArray, JsValue, Json}

class BinanceJsonConverterTest_Orders extends BasicTest {

  private def convertOrder(json: JsValue) = new BinanceJsonConverter().convertSingleOrder(json)

  private def convertOrders(json: JsValue) = new BinanceJsonConverter().convertOrders(json)

  private def orderJson
  (
    symbol: String = "",
    orderId: Int = 1,
    clientOrderId: String = "",
    price: String = "1",
    origQty: String = "1",
    executedQty: String = "1",
    `type`: String = "T",
    side: String = "BUY",
  ) =
    Json.parse(
      s"""{
         |"symbol": "$symbol",
         |"orderId": ${orderId.toString},
         |"orderListId": -1,
         |"clientOrderId": "$clientOrderId",
         |"price": "$price",
         |"origQty": "$origQty",
         |"executedQty": "$executedQty",
         |"cummulativeQuoteQty": "0.0",
         |"status": "NEW",
         |"timeInForce": "GTC",
         |"type": "${`type`}",
         |"side": "$side",
         |"stopPrice": "0.0",
         |"icebergQty": "0.0",
         |"time": 1499827319559,
         |"updateTime": 1499827319559,
         |"isWorking": true,
         |"origQuoteOrderQty": "0.000000"
         |}""".stripMargin)

  test("symbol and order ids are just taken as they are (order id as string)") {
    convertOrder(orderJson(symbol = "ASDFJK")).symbol shouldEqual "ASDFJK"
    convertOrder(orderJson(orderId = 123)).id shouldEqual "123"
    convertOrder(orderJson(clientOrderId = "C123")).clientOrderId shouldEqual "C123"
  }

  test("price and quantities are read as big decimals") {
    convertOrder(orderJson(price = "1.23")).price shouldEqual dec("1.23")
    convertOrder(orderJson(origQty = "2.34")).originalQuantity shouldEqual dec("2.34")
    convertOrder(orderJson(executedQty = "3.45")).executedQuantity shouldEqual dec("3.45")
  }

  test("the type is read as a string") {
    convertOrder(orderJson(`type` = "T123")).`type` shouldEqual "T123"
  }

  test("the side is properly converted") {
    convertOrder(orderJson(side = "BUY")).side shouldEqual Side.Buy
    convertOrder(orderJson(side = "SELL")).side shouldEqual Side.Sell
  }

  test("it can parse serveral orders in an array") {
    convertOrders(JsArray(Seq(orderJson(orderId = 12), orderJson(orderId = 34))))
      .map(_.id) shouldEqual Seq("12", "34")
  }

}
