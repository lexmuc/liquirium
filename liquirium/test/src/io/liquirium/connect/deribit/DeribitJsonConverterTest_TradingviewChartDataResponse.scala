package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.DeribitTradingviewChartDataResponse.Status
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json

import java.time.Instant

class DeribitJsonConverterTest_TradingviewChartDataResponse extends BasicTest {

  private def convert(s: String) = new DeribitJsonConverter().convertTradingviewCharDataResponse(Json.parse(s))

  test("a single candle is read with all fields and status is converted as well") {
    convert(
      """{
      "status":"ok",
      "ticks":[1517051940],
      "open":[1.0],
      "close":[0.5],
      "high":[2.0],
      "low":[0.1],
      "volume":[123.45],
      "cost":[12345]
    }""") shouldEqual DeribitTradingviewChartDataResponse(
      Status.Ok,
      Seq(DeribitCandle(
        tick = Instant.ofEpochMilli(1517051940),
        open = BigDecimal("1"),
        close = BigDecimal("0.5"),
        high = BigDecimal("2.0"),
        low = BigDecimal("0.1"),
        volume = BigDecimal("123.45"),
        cost = BigDecimal("12345")
      )))
  }

  test("it can convert several candles") {
    convert(
      """{
      "status":"ok",
      "ticks":[111, 222],
      "volume":[123.45, 123.45],
      "open":[1.0, 1.0],
      "close":[0.5, 0.5],
      "high":[2.0, 2.0],
      "low":[0.1, 0.1],
      "cost": [1, 2]
    }""").candles.map(_.tick.toEpochMilli) shouldEqual Seq(111, 222)
  }

  test("if there is no data, the arrays may be omitted") {
    convert("""{"status":"no_data"}""") shouldEqual DeribitTradingviewChartDataResponse(Status.NoData, Seq())
  }

}
