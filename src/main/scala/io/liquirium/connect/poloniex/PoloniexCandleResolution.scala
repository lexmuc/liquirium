package io.liquirium.connect.poloniex

import java.time.Duration

object PoloniexCandleResolution {
  val fiveMinutes: PoloniexCandleResolution = PoloniexCandleResolution(300)
  val fifteenMinutes: PoloniexCandleResolution = PoloniexCandleResolution(900)
  val thirtyMinutes: PoloniexCandleResolution = PoloniexCandleResolution(1800)
  val twoHours: PoloniexCandleResolution = PoloniexCandleResolution(7200)
  val fourHours: PoloniexCandleResolution = PoloniexCandleResolution(14400)
  val oneDay: PoloniexCandleResolution = PoloniexCandleResolution(86400)

  val all = Set(fiveMinutes, fifteenMinutes, thirtyMinutes, twoHours, fourHours, oneDay)

  def forDuration(d: Duration): PoloniexCandleResolution = all.find(_.candleLength == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported Poloniex candle resolution: $d")
  }

}

case class PoloniexCandleResolution(seconds: Long) {

  val candleLength: Duration = Duration.ofSeconds(seconds)

}
