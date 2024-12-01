package io.liquirium.connect.binance

import io.liquirium.core.Side
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import org.scalatest.matchers.should.Matchers.{a, an, convertToAnyShouldWrapper, thrownBy}
import play.api.libs.json.{JsValue, Json}

class BinanceJsonConverterTest_StreamPayload extends BasicTest {

  private def convertPayload(json: JsValue) = new BinanceJsonConverter().convertPayload(json)

  private def convertExecutionReport(json: JsValue) =
    new BinanceJsonConverter().convertPayload(json).asInstanceOf[BinanceExecutionReport]

  private def executionReportJson
  (
    eventTime: Long = 0,
    transactionTime: Long = 0,
    symbol: String = "",
    clientOrderId: String = "",
    orderId: Long = 0L,
    tradeId: Long = 0L,
    side: String = "BUY",
    orderType: String = "",
    orderQuantity: String = "0",
    orderPrice: String = "0",
    lastExecutedQuantity: String = "0",
    lastExecutedPrice: String = "0",
    commissionAmount: String = "0",
    commissionAsset: String = "XXX",
    executionType: String = "NEW",
  ) = Json.parse(
    s"""{
       |"e": "executionReport",
       |"E": $eventTime,
       |"s": "$symbol",
       |"c": "$clientOrderId",
       |"S": "$side",
       |"o": "$orderType",
       |"f": "GTC",
       |"q": "$orderQuantity",
       |"p": "$orderPrice",
       |"P": "0.00000000",
       |"F": "0.00000000",
       |"g": -1,
       |"C": "",
       |"x": "$executionType",
       |"X": "NEW",
       |"r": "NONE",
       |"i": $orderId,
       |"l": "$lastExecutedQuantity",
       |"z": "0.00000000",
       |"L": "$lastExecutedPrice",
       |"n": "$commissionAmount",
       |"N": ${if (commissionAsset == null) "null" else "\"" + commissionAsset + "\""},
       |"T": $transactionTime,
       |"t": $tradeId,
       |"I": 8641984,
       |"w": true,
       |"m": false,
       |"M": false,
       |"O": 1499405658657,
       |"Z": "0.00000000",
       |"Y": "0.00000000",
       |"Q": "0.00000000"
       |}""".stripMargin)

  test("event types other than the execution report are converted to irrelevant payload objects") {
    convertPayload(Json.parse("""{ "e": "irrelevantEvent" }""")) shouldEqual IrrelevantPayload("irrelevantEvent")
  }

  test("execution reports are returned as such") {
    convertPayload(executionReportJson()) shouldBe a[BinanceExecutionReport]
  }

  test("event time and transaction time are assigned as given") {
    val er = convertExecutionReport(executionReportJson(eventTime = 123L, transactionTime = 234L))
    er.eventTime shouldEqual 123L
    er.transactionTime shouldEqual 234L
  }

  test("symbol and client order id are assigned as strings") {
    val er = convertExecutionReport(executionReportJson(symbol = "AAABBB", clientOrderId = "CID123"))
    er.symbol shouldEqual "AAABBB"
    er.clientOrderId shouldEqual "CID123"
  }

  test("the side is converted to the Side enum") {
    convertExecutionReport(executionReportJson(side = "BUY")).side shouldEqual Side.Buy
    convertExecutionReport(executionReportJson(side = "SELL")).side shouldEqual Side.Sell
  }

  test("the order side is assigned as a string") {
    convertExecutionReport(executionReportJson(orderType = "XYZ")).orderType shouldEqual "XYZ"
  }

  test("all quantity and price fields are assigned as big decimals") {
    val er = convertExecutionReport(executionReportJson(
      orderPrice = "1.2",
      orderQuantity = "2.3",
      lastExecutedPrice = "3.4",
      lastExecutedQuantity = "4.5",
      commissionAmount = "5.6",
    ))
    er.orderPrice shouldEqual dec("1.2")
    er.orderQuantity shouldEqual dec("2.3")
    er.lastExecutedPrice shouldEqual dec("3.4")
    er.lastExecutedQuantity shouldEqual dec("4.5")
    er.commissionAmount shouldEqual dec("5.6")
  }

  test("the execution types are all recognized and an unknown execution type yields an exception") {
    def er(s: String) = convertExecutionReport(executionReportJson(executionType = s))
    er("NEW").currentExecutionType shouldEqual BinanceExecutionType.NEW
    er("TRADE").currentExecutionType shouldEqual BinanceExecutionType.TRADE
    er("CANCELED").currentExecutionType shouldEqual BinanceExecutionType.CANCELED
    er("EXPIRED").currentExecutionType shouldEqual BinanceExecutionType.EXPIRED
    er("REJECTED").currentExecutionType shouldEqual BinanceExecutionType.REJECTED
    er("REPLACED").currentExecutionType shouldEqual BinanceExecutionType.REPLACED
    an[Exception] shouldBe thrownBy(er("FAILURE"))
  }

  test("the commission asset is an optional string(can be null)") {
    convertExecutionReport(executionReportJson(commissionAsset = "XYZ")).commissionAsset shouldEqual Some("XYZ")
    convertExecutionReport(executionReportJson(commissionAsset = null)).commissionAsset shouldEqual None
  }

  test("the order id is simply assigned") {
    convertExecutionReport(executionReportJson(orderId = 345)).orderId shouldEqual 345
  }

  test("the trade id is an optional Long and -1 yields None") {
    convertExecutionReport(executionReportJson(tradeId = -1)).tradeId shouldEqual None
    convertExecutionReport(executionReportJson(tradeId = 123)).tradeId shouldEqual Some(123)
  }

}
