package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.helpers.SimulationHelpers
import io.liquirium.util.JsonSerializer
import io.liquirium.util.helpers.JsonSerializerTest

class ChartDataSeriesConfigJsonSerializerTest extends JsonSerializerTest[ChartDataSeriesConfig] {

  override def buildSerializer(): JsonSerializer[ChartDataSeriesConfig] =
    new ChartDataSeriesConfigJsonSerializer()

  test("it serializes the relevant fields of a config with line appearance") {
    assertSerialization(
      SimulationHelpers.makeChartDataSeriesConfig(
        precision = 8,
        caption = "Simple series",
        appearance = LineAppearance(
          lineWidth = 2,
          color = "black",
          overlay = true,
        ),
      ),
      """{
          "precision" : 8,
          "caption" : "Simple series",
          "appearance" : {
            "type" : "line",
            "lineWidth" : 2,
            "color" : "black",
            "overlay" : true
          }
        }"""
    )
  }

  test("it serializes the relevant fields of a config with histogram appearance") {
    assertSerialization(
      SimulationHelpers.makeChartDataSeriesConfig(
        precision = 8,
        caption = "Simple series",
        appearance = HistogramAppearance(
          color = "blue",
        ),
      ),
      """{
          "precision" : 8,
          "caption" : "Simple series",
          "appearance" : {
            "type" : "histogram",
            "color" : "blue"
          }
        }"""
    )
  }

}
