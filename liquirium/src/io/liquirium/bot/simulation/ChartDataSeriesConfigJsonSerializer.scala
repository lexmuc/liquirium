package io.liquirium.bot.simulation

import io.liquirium.util.JsonSerializer
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString}

class ChartDataSeriesConfigJsonSerializer extends JsonSerializer[ChartDataSeriesConfig] {

  def serialize(c: ChartDataSeriesConfig): JsObject =
    JsObject(Map(
      "precision" -> JsNumber(c.precision),
      "caption" -> JsString(c.caption),
      "appearance" -> serializeAppearance(c.appearance),
    ))

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
}
