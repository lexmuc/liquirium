package io.liquirium.connect.coinbase

import io.liquirium.core.Side
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}
import play.api.libs.json.Json

class CoinbaseJsonConverterTest_Trades extends BasicTest {

  private def tradeJson(
    entryId: String = "e123",
    tradeId: String = "t456",
    orderId: String = "o789",
    tradeTime: String = "2021-05-31T09:59:59Z",
    tradeType: String = "FILL",
    price: String = "10.000",
    size: String = "000.1",
    commission: String = "1.25",
    productId: String = "BTC-USD",
    sequenceTimestamp: String = "2021-05-31T09:58:59Z",
    side: String = "BUY",
  ) =
    s"""{
       |  "entry_id": "$entryId",
       |  "trade_id": "$tradeId",
       |  "order_id": "$orderId",
       |  "trade_time": "$tradeTime",
       |  "trade_type": "$tradeType",
       |  "price": "$price",
       |  "size": "$size",
       |  "commission": "$commission",
       |  "product_id": "$productId",
       |  "sequence_timestamp": "$sequenceTimestamp",
       |  "side": "$side"
       |}""".stripMargin

  val converter = new CoinbaseJsonConverter()

  private def convert(s: String) = converter.convertSingleTrade(Json.parse(s))

  private def convertMany(s: String) = converter.convertTrades(Json.parse(s))

  test("trades are returned with correct entryId, tradeId, orderId, ") {
    convert(tradeJson(entryId = "exxx")).entryId shouldEqual "exxx"
    convert(tradeJson(tradeId = "txxx")).tradeId shouldEqual "txxx"
    convert(tradeJson(orderId = "oxxx")).orderId shouldEqual "oxxx"
  }

  test("trades are returned with correct trade time and sequence timestamp parsed as an Instant") {
    convert(tradeJson(tradeTime = "1970-01-01T00:00:01Z")).tradeTime shouldEqual sec(1)
    convert(tradeJson(sequenceTimestamp = "1970-01-01T00:00:02Z")).sequenceTimestamp shouldEqual sec(2)
  }

  test("trades are returned with correct trade type") {
    convert(tradeJson(tradeType = "FILL")).tradeType shouldEqual "FILL"
  }

  test("trades are returned with correct price, size, commission") {
    convert(tradeJson(price = "5456.297")).price shouldEqual dec("5456.297")
    convert(tradeJson(size = "0.0578")).size shouldEqual dec("0.0578")
    convert(tradeJson(commission = "11.87")).commission shouldEqual dec("11.87")
  }

  test("trades are returned with correct product id") {
    convert(tradeJson(productId = "ETH-USD")).productId shouldEqual "ETH-USD"
  }

  test("trades are returned with correct side") {
    convert(tradeJson(side = "SELL")).side shouldEqual Side.Sell
  }

  test("an exception is thrown when the trade type is not 'FILL'") {
    an[Exception] shouldBe thrownBy(convert(tradeJson(tradeType = "notFILL")))
  }

  test("several trades can be parsed at once") {
    convertMany("[" + tradeJson(tradeId = "1") + "," + tradeJson(tradeId = "2") + "]")
      .map(_.tradeId) shouldEqual Seq("1", "2")
  }

}
