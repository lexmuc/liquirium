package io.liquirium.bot.simulation

import io.liquirium.core.Market
import io.liquirium.util.JsonSerializer
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue}

class ChartDataJsonSerializer(
  dataSeriesConfigSerializer: JsonSerializer[ChartDataSeriesConfig],
) extends JsonSerializer[Seq[(Market, ChartDataLogger)]]{

  def serialize(marketsWithChartData: Seq[(Market, ChartDataLogger)]): JsValue =
    JsObject(
      marketsWithChartData.map {
        case (market, marketLogger) =>
          marketName(market) -> JsObject(Map(
            "candleData" -> getCandlesJson(marketLogger),
            "dataSeries" -> getDataSeriesJson(marketLogger),
          ))
      }
    )

  private def marketName(m: Market) = m.exchangeId.value + "-" + m.tradingPair.base + "-" + m.tradingPair.quote

  private def getDataSeriesJson(logger: ChartDataLogger): JsArray =
    JsArray(logger.dataSeriesConfigs.zipWithIndex.map {
      case (config, index) =>
        val seriesData = logger.chartDataUpdates.map(u => JsNumber(u.namedDataPoints(index))).toSeq
        JsObject(Map(
          "config" -> dataSeriesConfigSerializer.serialize(config),
          "data" -> JsArray(seriesData),
        ))
    })

  private def getCandlesJson(visualizationLogger: ChartDataLogger): JsArray =
    JsArray(
      visualizationLogger.chartDataUpdates.map { e =>
        if (e.candle.open > BigDecimal(0)) {
          JsObject(Map(
            "open" -> JsNumber(e.candle.open),
            "close" -> JsNumber(e.candle.close),
            "high" -> JsNumber(e.candle.high),
            "low" -> JsNumber(e.candle.low),
            "time" -> JsNumber(e.candle.startTime.getEpochSecond)
          ))
        }
        else {
          JsObject(Map(
            "time" -> JsNumber(e.candle.startTime.getEpochSecond)
          ))
        }
      }.toSeq
    )
}
