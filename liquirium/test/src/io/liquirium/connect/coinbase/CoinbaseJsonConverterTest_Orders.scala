package io.liquirium.connect.coinbase

import io.liquirium.core.Side
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsValue, Json}

//noinspection RedundantDefaultArgument
class CoinbaseJsonConverterTest_Orders extends BasicTest {

  private def orderJson(
    orderId: String = "123",
    productId: String = "ABC-DEF",
    fullQuantity: String = "100",
    filledQuantity: String = "10",
    side: String = "SELL",
    price: String = "5",
  ) = Json.obj(
    "order_id" -> s"$orderId",
    "product_id" -> s"$productId",
    "order_configuration" -> Json.obj(
      "limit_limit_gtc" -> Json.obj(
        "base_size" -> s"$fullQuantity",
        "limit_price" -> s"$price",
      ),
    ),
    "filled_size" -> s"$filledQuantity",
    "side" -> s"$side",
  )

  val converter = new CoinbaseJsonConverter()

  private def convert(json: JsValue) = converter.convertSingleOrder(json)

  private def convertMany(json: JsValue) = converter.convertOrders(json)


  test("the order id is returned as a string") {
    convert(orderJson(orderId = "4567")).orderId shouldEqual "4567"
  }

  test("the product id is returned as a string") {
    convert(orderJson(productId = "ABC-DEF")).productId shouldEqual "ABC-DEF"
  }

  test("full and fill quantity as well as price are returned as a decimal") {
    val o = convert(orderJson(fullQuantity = "5.23", filledQuantity = "2.34", price = "1.56"))
    o.fullQuantity shouldEqual dec("5.23")
    o.filledQuantity shouldEqual dec("2.34")
    o.price shouldEqual dec("1.56")
  }

  test("the side is returned as liquirium Side") {
    convert(orderJson(side = "BUY")).side shouldEqual Side.Buy
  }

  test("several orders can be parsed at once") {
    convertMany(JsArray(IndexedSeq(orderJson("a"), orderJson("b"))))
      .map(_.orderId) shouldEqual List("a", "b")
  }

}
