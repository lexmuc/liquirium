package io.liquirium.connect.binance

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.dec
import play.api.libs.json.{JsArray, JsValue, Json}

import java.time.Instant


class BinanceJsonConverterTest_Trades extends BasicTest {

  private def convertTrade(json: JsValue) = new BinanceJsonConverter().convertSingleTrade(json)

  private def convertTrades(json: JsValue) = new BinanceJsonConverter().convertTrades(json)

  private def tradeJson
  (
    id: Long = 1,
    symbol: String = "",
    orderId: Long = 1,
    price: String = "1",
    quantity: String = "1",
    commission: String = "0",
    commissionAsset: String = "BNB",
    isBuyer: Boolean = false,
    time: Long = 0,
  ) = Json.parse(
    s"""{
       |"symbol": "$symbol",
       |"id": ${ id.toString },
       |"orderId": ${ orderId.toString },
       |"orderListId": -1,
       |"price": "$price",
       |"qty": "$quantity",
       |"commission": "$commission",
       |"commissionAsset": "$commissionAsset",
       |"time": ${ time.toString },
       |"isBuyer": ${ if (isBuyer) "true" else "false" },
       |"isMaker": false,
       |"isBestMatch": true
       |}""".stripMargin)

  test("symbol and order ids are just taken as they are (order id as string)") {
    convertTrade(tradeJson(symbol = "ASDFJK")).symbol shouldEqual "ASDFJK"
    convertTrade(tradeJson(id = 100100100100L)).id shouldEqual "100100100100"
    convertTrade(tradeJson(orderId = 200200200200L)).orderId shouldEqual "200200200200"
  }

  test("price, quantity and commission are read as big decimals") {
    convertTrade(tradeJson(price = "1.23")).price shouldEqual dec("1.23")
    convertTrade(tradeJson(quantity = "2.34")).quantity shouldEqual dec("2.34")
    convertTrade(tradeJson(commission = "3.45")).commission shouldEqual dec("3.45")
  }

  test("the commission asset is taken as-is") {
    convertTrade(tradeJson(commissionAsset = "XYZ")).commissionAsset shouldEqual "XYZ"
  }

  test("the time is converted to an instant (milliseconds assumed)") {
    convertTrade(tradeJson(time = 1234000)).time shouldEqual Instant.ofEpochMilli(1234000)
  }

  test("the isBuyer flag is just read as a boolean") {
    convertTrade(tradeJson(isBuyer = false)).isBuyer shouldEqual false
    convertTrade(tradeJson(isBuyer = true)).isBuyer shouldEqual true
  }

  test("it can parse serveral trades in an array") {
    convertTrades(JsArray(Seq(tradeJson(id = 12), tradeJson(id = 34))))
      .map(_.id) shouldEqual Seq("12", "34")
  }

}
