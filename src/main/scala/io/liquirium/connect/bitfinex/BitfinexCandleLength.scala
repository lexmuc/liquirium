package io.liquirium.connect.bitfinex

import java.time.Duration

object BitfinexCandleLength {

  val oneMinute: BitfinexCandleLength = BitfinexCandleLength("1m", 60)
  val fiveMinutes: BitfinexCandleLength = BitfinexCandleLength("5m", 60 * 5)
  val fifteenMinutes: BitfinexCandleLength = BitfinexCandleLength("15m", 60 * 15)
  val thirtyMinutes: BitfinexCandleLength = BitfinexCandleLength("30m", 60 * 30)
  val oneHour: BitfinexCandleLength = BitfinexCandleLength("1h", 60 * 60)
  val threeHours: BitfinexCandleLength = BitfinexCandleLength("3h", oneHour.seconds * 3)
  val sixHours: BitfinexCandleLength = BitfinexCandleLength("6h", oneHour.seconds * 6)
  val twelveHours: BitfinexCandleLength = BitfinexCandleLength("12h", oneHour.seconds * 12)
  val oneDay: BitfinexCandleLength = BitfinexCandleLength("1D", oneHour.seconds * 24)
  val sevenDays: BitfinexCandleLength = BitfinexCandleLength("7D", oneDay.seconds * 7)
  val fourteenDays: BitfinexCandleLength = BitfinexCandleLength("14D", oneDay.seconds * 14)
  // not used because one month is not a fix length
//  val oneMonth: BitfinexCandleLength = BitfinexCandleLength("1M", oneDay.seconds * 30)

  val all = Set(
    oneMinute,
    fiveMinutes,
    fifteenMinutes,
    thirtyMinutes,
    oneHour,
    threeHours,
    sixHours,
    twelveHours,
    oneDay,
    sevenDays,
    fourteenDays,
  )

  def forDuration(d: Duration): BitfinexCandleLength = all.find(_.candleLength == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported candle length: $d")
  }

}

case class BitfinexCandleLength(code: String, seconds: Long) {

  val candleLength: Duration = Duration.ofSeconds(seconds)

}
