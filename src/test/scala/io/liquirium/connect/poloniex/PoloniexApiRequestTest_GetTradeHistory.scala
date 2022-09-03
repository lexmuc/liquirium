package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetTradeHistory
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PrivateGet
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.{poloniexTrade => trade}
import io.liquirium.core.helpers.CoreHelpers.milli
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue

import java.time.Instant


class PoloniexApiRequestTest_GetTradeHistory extends TestWithMocks {

  private def getTrades(
    limit: Option[Int] = None,
    endTime: Option[Instant] = None,
    startTime: Option[Instant] = None,
    from: Option[Long] = None,
    direction: Option[String] = None,
    symbols: List[String] = List()
  ) = GetTradeHistory(
    limit = limit,
    endTime = endTime,
    startTime = startTime,
    from = from,
    direction = direction,
    symbols = symbols
  )

  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  private def fakeSeqConversion(json: JsValue, trades: Seq[PoloniexTrade]) =
    jsonConverter.convertTrades(json) returns trades

  test("the http request is a public GET request") {
    getTrades().httpRequest shouldBe a[PrivateGet]
  }

  test("a limit is set as a parameter when given") {
    getTrades(limit = Some(500)).httpRequest.params should contain("limit", "500")
  }

  test("the startTime and endTime are set when given") {
    val r = getTrades(startTime = Some(milli(1000)), endTime = Some(milli(3000)))
    r.httpRequest.params should contain("startTime", "1000")
    r.httpRequest.params should contain("endTime", "3000")
  }

  test("from is set when given") {
    getTrades(from = Some(123456789)).httpRequest.params should contain("from", "123456789")
  }

  test("direction is set when given") {
    getTrades(direction = Some("PRE")).httpRequest.params should contain("direction", "PRE")
  }

  test("the result is parsed as a sequence of trades") {
    fakeSeqConversion(json(123), Seq(trade("X"), trade("Y")))
    getTrades().convertResponse(json(123), jsonConverter) shouldEqual Seq(trade("X"), trade("Y"))
  }

  test("if passed the symbols are included in the parameters") {
    getTrades(symbols = List("ASDFJK")).httpRequest.params should contain("symbols", "ASDFJK")
  }

  test("if not passed symbols are not included in the parameters") {
    getTrades().httpRequest.params should not contain("symbols", "")
  }
}
