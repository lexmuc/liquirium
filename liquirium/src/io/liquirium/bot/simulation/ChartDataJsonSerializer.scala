package io.liquirium.bot.simulation

import io.liquirium.core.Market
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

class ChartDataJsonSerializer {

  def serialize(chartDataByMarket: Map[Market, ChartDataLogger]): JsValue =
    JsObject(
      chartDataByMarket.map {
        case (market, marketLogger) =>
          marketName(market) -> JsObject(Map(
            "candleData" -> getCandlesJson(marketLogger),
            "dataSeries" -> getDataSeriesJson(marketLogger),
          ))
      }
    )

  private def marketName(m: Market) = m.exchangeId.value + "-" + m.tradingPair.base + "-" + m.tradingPair.quote

  private def serializeDataSeriesConfig(c: ChartDataSeriesConfig): JsObject = {
    JsObject(Map(
      "precision" -> JsNumber(c.precision),
      "caption" -> JsString(c.caption),
      "appearance" -> serializeAppearance(c.appearance),
    ))
  }

  private def serializeAppearance(a: ChartDataSeriesAppearance): JsObject = {
    a match {
      case l: LineAppearance =>
        JsObject(Map(
          "type" -> JsString("line"),
          "lineWidth" -> JsNumber(l.lineWidth),
          "color" -> JsString(l.color),
          "overlay" -> JsBoolean(l.overlay),
        ))
      case h: HistogramAppearance =>
        JsObject(Map(
          "type" -> JsString("histogram"),
          "color" -> JsString(h.color),
        ))
    }
  }

  private def getDataSeriesJson(logger: ChartDataLogger): JsArray =
    JsArray(logger.dataSeriesConfigs.zipWithIndex.map {
      case (config, index) =>
        val seriesData = logger.chartDataUpdates.map(u => JsNumber(u.namedDataPoints(index))).toSeq
        JsObject(Map(
          "config" -> serializeDataSeriesConfig(config),
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
