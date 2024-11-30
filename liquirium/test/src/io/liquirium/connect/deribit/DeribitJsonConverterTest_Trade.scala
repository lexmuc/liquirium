package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.DeribitDirection.{Buy, Sell}
import io.liquirium.connect.deribit.helpers.DeribitJsonHelpers.deribitTradeJson
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsValue, Json}

class DeribitJsonConverterTest_Trade extends BasicTest {

  private def convertSingle(json: JsValue) = new DeribitJsonConverter().convertTrade(json)

  private def convertMany(json: JsValue*) = new DeribitJsonConverter().convertTrades(JsArray(json))

  private def convertResponse(json: JsValue) = new DeribitJsonConverter().convertTradesResponse(json)

  private def responseJson(hasMore: String = "true")(trades: JsValue*) = Json.parse(
    s"""{
        "has_more": $hasMore,
        "trades": [${ trades.map(_.toString).mkString(",") }]
    }"""
  )

  test("id, order id and sequence number are set on the trade") {
    val t = convertSingle(deribitTradeJson(id = "id123", orderId = "oid123", sequenceNumber = 321))
    t.id shouldEqual "id123"
    t.orderId shouldEqual "oid123"
    t.sequenceNumber shouldEqual 321
  }

  test("prices are converted as big decimals") {
    val s = convertSingle(deribitTradeJson(indexPrice = dec("123.34"), price = dec("234.56")))
    s.indexPrice shouldEqual BigDecimal("123.34")
    s.price shouldEqual BigDecimal("234.56")
  }

  test("fee and quantity are converted as big decimals") {
    val s = convertSingle(deribitTradeJson(fee = dec("123.34"), quantity = dec("234.56")))
    s.fee shouldEqual BigDecimal("123.34")
    s.quantity shouldEqual BigDecimal("234.56")
  }

  test("fee currency is just set on the trade") {
    convertSingle(deribitTradeJson(feeCurrency = "XYZ")).feeCurrency shouldEqual "XYZ"
  }

  test("the instrument name is set on the trade") {
    convertSingle(deribitTradeJson(instrument = "inst123")).instrument shouldEqual "inst123"
  }

  test("the direction is set") {
    convertSingle(deribitTradeJson(direction = "buy")).direction shouldEqual Buy
    convertSingle(deribitTradeJson(direction = "sell")).direction shouldEqual Sell
  }

  test("the timestamp is parsed as a Long value") {
    convertSingle(deribitTradeJson(timestamp = 12345L)).timestamp shouldEqual 12345L
  }

  test("several trades can be parsed at once") {
    convertMany(deribitTradeJson(timestamp = 12), deribitTradeJson(timestamp = 34))
      .map(_.timestamp) shouldEqual Seq(12, 34)
  }

  test("it parses a trades result as a sequence of trades and a 'hasMore' flag") {
    val r = convertResponse(responseJson("true")(deribitTradeJson(timestamp = 12), deribitTradeJson(timestamp = 34)))
    r.hasMore shouldBe true
    r.trades.map(_.timestamp) shouldEqual Seq(12, 34)
    convertResponse(responseJson("false")()).hasMore shouldBe false
  }

}
