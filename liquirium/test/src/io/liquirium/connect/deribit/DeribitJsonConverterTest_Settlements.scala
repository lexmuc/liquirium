package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.helpers.DeribitTestHelpers.continuationToken
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}
import play.api.libs.json.{JsArray, JsValue, Json}

class DeribitJsonConverterTest_Settlements extends BasicTest {

  private def convertSingle(json: JsValue) = new DeribitJsonConverter().convertSettlement(json)

  private def convertMany(json: JsValue*) = new DeribitJsonConverter().convertSettlements(JsArray(json))

  private def convertResponse(json: JsValue) = new DeribitJsonConverter().convertSettlementResponse(json)

  private def settlementJson(
    indexPrice: String = "0",
    instrumentName: String = "",
    markPrice: String = "0",
    position: String = "0",
    profitLoss: String = "0",
    sessionProfitLoss: String = "0",
    timestamp: Long = 0,
    `type`: String = "settlement"
  ) = Json.parse(
    s"""{
       |  "index_price": $indexPrice,
       |  "instrument_name": "$instrumentName",
       |  "mark_price": $markPrice,
       |  "position": $position,
       |  "profit_loss": $profitLoss,
       |  "session_profit_loss": $sessionProfitLoss,
       |  "timestamp": ${ timestamp.toString },
       |  "type": "${ `type` }"
       |}""".stripMargin
  )

  private def responseJson(continuation: String = "")(settlements: JsValue*) = Json.parse(
    s"""{
        "continuation": "$continuation",
        "settlements": [${ settlements.map(_.toString).mkString(",") }]
    }"""
  )

  test("prices are converted as big decimals") {
    val s = convertSingle(settlementJson(indexPrice = "123.34", markPrice = "234.56"))
    s.indexPrice shouldEqual BigDecimal("123.34")
    s.markPrice shouldEqual BigDecimal("234.56")
  }

  test("the instrument name is set on the settlement") {
    convertSingle(settlementJson(instrumentName = "inst123")).instrumentName shouldEqual "inst123"
  }

  test("position, profitLoss and sessionProfitLoss are parsed to BigDecimal values") {
    val s = convertSingle(settlementJson(position = "-123.34", profitLoss = "-234.56", sessionProfitLoss = "345.67"))
    s.position shouldEqual BigDecimal("-123.34")
    s.profitLoss shouldEqual BigDecimal("-234.56")
    s.sessionProfitLoss shouldEqual BigDecimal("345.67")
  }

  test("the timestamp is parsed as a Long value") {
    convertSingle(settlementJson(timestamp = 12345L)).timestamp shouldEqual 12345L
  }

  test("the type is either settlement or delivery") {
    convertSingle(settlementJson(`type` = "settlement")).`type` shouldEqual DeribitSettlement.Settlement
    convertSingle(settlementJson(`type` = "delivery")).`type` shouldEqual DeribitSettlement.Delivery
    an[Exception] shouldBe thrownBy(convertSingle(settlementJson(`type` = "xyz")))
  }

  test("several settlements can be parsed at once") {
    convertMany(settlementJson(timestamp = 12), settlementJson(timestamp = 34)).map(_.timestamp) shouldEqual Seq(12, 34)
  }

  test("it parses a settlement result as a sequence of settlements and a continuation token") {
    val r = convertResponse(responseJson("cont123")(settlementJson(timestamp = 12), settlementJson(timestamp = 34)))
    r.continuationToken shouldEqual Some(continuationToken("cont123"))
    r.settlements.map(_.timestamp) shouldEqual Seq(12, 34)
  }

  test("the continuation token is None if its value is 'none'") {
    convertResponse(responseJson("none")(settlementJson(timestamp = 12))).continuationToken shouldEqual None
  }

}
