package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.helpers.SimulationHelpers.makeChartDataSeriesConfig
import io.liquirium.core.Market
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{CandleHelpers, MarketHelpers, TestWithMocks}
import io.liquirium.util.JsonSerializer
import io.liquirium.util.helpers.{FakeJsonSerializer, JsonSerializerTest}
import org.mockito.Mockito.mock
import play.api.libs.json.{JsString, JsValue}

class ChartDataJsonSerializerTest extends JsonSerializerTest[Seq[(Market, ChartDataLogger)]] with TestWithMocks {

  var configMappings: Seq[(ChartDataSeriesConfig, JsValue)] = Seq[(ChartDataSeriesConfig, JsValue)]()

  override def buildSerializer(): JsonSerializer[Seq[(Market, ChartDataLogger)]] =
    new ChartDataJsonSerializer(
      FakeJsonSerializer[ChartDataSeriesConfig](configMappings: _*)
    )

  test("it serializes chart data for a single market with candles and data series, market is serialized with dashes") {
    val market = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE1", "QUOTE1")
    val dataSeriesConfig1 = makeChartDataSeriesConfig(caption = "config1")
    val dataSeriesConfig2 = makeChartDataSeriesConfig(caption = "config2")
    configMappings = configMappings :+ (dataSeriesConfig1, JsString("config1"))
    configMappings = configMappings :+ (dataSeriesConfig2, JsString("config2"))
    val fakeLogger = mock(classOf[ChartDataLogger])
    fakeLogger.dataSeriesConfigs returns Seq(dataSeriesConfig1, dataSeriesConfig2)
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
        |        "config": "config1",
        |        "data": [
        |          1.1,
        |          1.2
        |        ]
        |      },
        |      {
        |        "config": "config2",
        |        "data": [
        |          2.1,
        |          2.2
        |        ]
        |      }
        |    ]
        |  }
        |}
        |""".stripMargin

    assertSerialization(Seq(market -> fakeLogger), expectedResultAsString)
  }

  test("it can serialize multiple markets at once") {
    val market1 = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE1", "QUOTE1")
    val market2 = MarketHelpers.market(MarketHelpers.exchangeId("EX123"), "BASE2", "QUOTE2")

    val fakeLogger1 = mock(classOf[ChartDataLogger])
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

    val fakeLogger2 = mock(classOf[ChartDataLogger])
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

    assertSerialization(Seq(market1 -> fakeLogger1, market2 -> fakeLogger2), expectedResultAsString)
  }

}
