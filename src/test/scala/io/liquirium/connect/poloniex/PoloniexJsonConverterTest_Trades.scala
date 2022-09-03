package io.liquirium.connect.poloniex

import io.liquirium.core.Side
import io.liquirium.core.helpers.BasicTest
import play.api.libs.json.Json

import java.time.Instant

//noinspection RedundantDefaultArgument
class PoloniexJsonConverterTest_Trades extends BasicTest {

  private def tradeJson(
    id: String = "abc",
    symbol: String = "BTC_USDT",
    accountType: String = "SPOT",
    orderId: String = "123",
    side: String = "BUY",
    `type`: String = "MARKET",
    matchRole: String = "MAKER",
    createTime: Long = 1000,
    price: String = "10.6",
    quantity: String = "0.5",
    amount: String = "5.3",
    feeCurrency: String = "USDT",
    feeAmount: String = "0.3",
    pageId: String = "123",
    clientOrderId: String = "ownId",
  ) =
    s"""{
       |  "id": "$id",
       |  "symbol": "$symbol",
       |  "accountType": "$accountType",
       |  "orderId": "$orderId",
       |  "side": "$side",
       |  "type": "${ `type` }",
       |  "matchRole": "$matchRole",
       |  "createTime": $createTime,
       |  "price": "$price",
       |  "quantity": "$quantity",
       |  "amount": "$amount",
       |  "feeCurrency": "$feeCurrency",
       |  "feeAmount": "$feeAmount",
       |  "pageId": "$pageId",
       |  "clientOrderId": "$clientOrderId"
       |}""".stripMargin

  val converter = new PoloniexJsonConverter()

  private def convert(s: String) = converter.convertTrade(Json.parse(s))

  private def convertMany(s: String) = converter.convertTrades(Json.parse(s))


  test("trades are returned with correct id") {
    convert(tradeJson(id = "123")).id shouldEqual "123"
    convert(tradeJson(id = "abcde")).id shouldEqual "abcde"
  }

  test("trades are returned with correct symbol") {
    convert(tradeJson(symbol = "ETH_USDT")).symbol shouldEqual "ETH_USDT"
  }

  test("trades are returned with correct accountType") {
    convert(tradeJson(accountType = "TYPE")).accountType shouldEqual "TYPE"
  }

  test("trades are returned with correct orderId") {
    convert(tradeJson(orderId = "32164923987566592")).orderId shouldEqual "32164923987566592"
  }

  test("trades are returned with correct side") {
    convert(tradeJson(side = "BUY")).side shouldEqual Side.Buy
    convert(tradeJson(side = "SELL")).side shouldEqual Side.Sell
  }

  test("it throws an exception when side is invalid") {
    an[Exception] shouldBe thrownBy(convert(tradeJson(side = "WRONG")))
  }

  test("trades are returned with correct type") {
    convert(tradeJson(`type` = "LIMIT")).`type` shouldEqual "LIMIT"
  }

  test("trades are returned with correct matchRole") {
    convert(tradeJson(matchRole = "TAKER")).matchRole shouldEqual "TAKER"
  }

  test("trades are returned with createTime parsed to an instant") {
    convert(tradeJson(createTime = 3600)).createTime shouldEqual Instant.ofEpochMilli(3600)
  }

  test("trades are returned with correct price, quantity and amount") {
    val t = convert(tradeJson(price = "20.8", quantity = "0.25", amount = "5.4"))
    t.price shouldEqual BigDecimal("20.8")
    t.quantity shouldEqual BigDecimal("0.25")
    t.amount shouldEqual BigDecimal("5.4")
  }

  test("trades are returned with correct feeCurrency") {
    convert(tradeJson(feeCurrency = "EUR")).feeCurrency shouldEqual "EUR"
  }

  test("trades are returned with correct feeAmount") {
    convert(tradeJson(feeAmount = "1.23")).feeAmount shouldEqual BigDecimal("1.23")
  }

  test("several trades can be parsed at once") {
    convertMany("[" + tradeJson(id = "1") + "," + tradeJson(id = "2") + "]")
      .map(_.id) shouldEqual Seq("1", "2")
  }

}
