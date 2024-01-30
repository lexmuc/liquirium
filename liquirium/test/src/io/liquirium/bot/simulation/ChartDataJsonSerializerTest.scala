package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.helpers.SimulationHelpers
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{CandleHelpers, MarketHelpers, TestWithMocks}
import play.api.libs.json.Json

class ChartDataJsonSerializerTest extends TestWithMocks {

  test("it serializes chart data for a single market with candles and data series, market is serialized with dashes") {
    val chartDataJsonSerializer = new ChartDataJsonSerializer()

    val market = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE1", "QUOTE1")

    val fakeLogger = mock[ChartDataLogger]
    fakeLogger.dataSeriesConfigs returns Seq(
      SimulationHelpers.makeChartDataSeriesConfig(
        caption = "TotalValue",
        precision = 8,
        appearance = LineAppearance(
          lineWidth = 3,
          color = "black",
          overlay = true,
        ),
      ),
      SimulationHelpers.makeChartDataSeriesConfig(
        caption = "Benchmark",
        precision = 2,
        appearance = HistogramAppearance(
          color = "blue",
        ),
      ),
    )
    fakeLogger.chartDataUpdates returns Seq(
      ChartDataUpdate(
        candle = CandleHelpers.candle(
          start = sec(123000),
          open = dec("0.05"),
          close = dec("0.06"),
          high = dec("0.07"),
          low = dec("0.04"),
        ),
        namedDataPoints = Map(
          0 -> BigDecimal("1.1"),
          1 -> BigDecimal("2.1"),
        ),
      ),
      ChartDataUpdate(
        candle = CandleHelpers.candle(
          start = sec(124000),
          open = dec("0.01"),
          close = dec("0.01"),
          high = dec("0.01"),
          low = dec("0.01"),
        ),
        namedDataPoints = Map(
          0 -> BigDecimal("1.2"),
          1 -> BigDecimal("2.2"),
        ),
      ),
    )

    val expectedResultAsString =
      """
        |{
        |  "EX123-BASE1-QUOTE1": {
        |    "candleData": [
        |      {
        |        "open": 0.05,
        |        "low": 0.04,
        |        "time": 123000,
        |        "close": 0.06,
        |        "high": 0.07
        |      },
        |      {
        |        "open": 0.01,
        |        "low": 0.01,
        |        "time": 124000,
        |        "close": 0.01,
        |        "high": 0.01
        |      }
        |    ],
        |    "dataSeries": [
        |      {
        |        "config": {
        |          "precision": 8,
        |          "caption": "TotalValue",
        |          "appearance": {
        |            "type": "line",
        |            "lineWidth": 3,
        |            "color": "black",
        |            "overlay": true
        |          }
        |        },
        |        "data": [
        |          1.1,
        |          1.2
        |        ]
        |      },
        |      {
        |        "config": {
        |          "precision": 2,
        |          "caption": "Benchmark",
        |          "appearance": {
        |            "type": "histogram",
        |            "color": "blue"
        |          }
        |        },
        |        "data": [
        |          2.1,
        |          2.2
        |        ]
        |      }
        |    ]
        |  }
        |}
        |""".stripMargin

    println(expectedResultAsString)

    val actualJson = chartDataJsonSerializer.serialize(Map(market -> fakeLogger))
    Json.prettyPrint(actualJson) shouldEqual Json.prettyPrint(Json.parse(expectedResultAsString))
  }

  test("it can serialize multiple markets at once") {

    val chartDataJsonSerializer = new ChartDataJsonSerializer()

    val market1 = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE1", "QUOTE1")
    val market2 = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE2", "QUOTE2")

    val fakeLogger1 = mock[ChartDataLogger]
    fakeLogger1.dataSeriesConfigs returns Seq()
    fakeLogger1.chartDataUpdates returns Seq(
      ChartDataUpdate(
        candle = CandleHelpers.candle(
          start = sec(123000),
          open = dec("0.05"),
          close = dec("0.06"),
          high = dec("0.07"),
          low = dec("0.04"),
        ),
        namedDataPoints = Map(),
      ),
    )

    val fakeLogger2 = mock[ChartDataLogger]
    fakeLogger2.dataSeriesConfigs returns Seq()
    fakeLogger2.chartDataUpdates returns Seq(
      ChartDataUpdate(
        candle = CandleHelpers.candle(
          start = sec(123000),
          open = dec("0.5"),
          close = dec("0.6"),
          high = dec("0.7"),
          low = dec("0.4"),
        ),
        namedDataPoints = Map(),
      ),
    )

    val expectedResultAsString =
      """
        |{
        |  "EX123-BASE1-QUOTE1": {
        |    "candleData": [
        |      {
        |        "open": 0.05,
        |        "low": 0.04,
        |        "time": 123000,
        |        "close": 0.06,
        |        "high": 0.07
        |      }
        |    ],
        |    "dataSeries": []
        |  },
        |  "EX123-BASE2-QUOTE2": {
        |    "candleData": [
        |      {
        |        "open": 0.5,
        |        "low": 0.4,
        |        "time": 123000,
        |        "close": 0.6,
        |        "high": 0.7
        |      }
        |    ],
        |    "dataSeries": []
        |  }
        |}
        |""".stripMargin

    println(expectedResultAsString)

    val actualJson = chartDataJsonSerializer.serialize(Map(market1 -> fakeLogger1, market2 -> fakeLogger2))
    Json.prettyPrint(actualJson) shouldEqual Json.prettyPrint(Json.parse(expectedResultAsString))
  }

}
