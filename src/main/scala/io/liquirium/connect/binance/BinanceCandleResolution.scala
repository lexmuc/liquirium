package io.liquirium.connect.binance

import java.time.Duration

object BinanceCandleResolution {

  val oneMinute: BinanceCandleResolution = BinanceCandleResolution("1m", Duration.ofMinutes(1))
  val threeMinutes: BinanceCandleResolution = BinanceCandleResolution("3m", Duration.ofMinutes(3))
  val fiveMinutes: BinanceCandleResolution = BinanceCandleResolution("5m", Duration.ofMinutes(5))
  val fifteenMinutes: BinanceCandleResolution = BinanceCandleResolution("15m", Duration.ofMinutes(15))
  val thirtyMinutes: BinanceCandleResolution = BinanceCandleResolution("30m", Duration.ofMinutes(30))
  val oneHour: BinanceCandleResolution = BinanceCandleResolution("1h", Duration.ofHours(1))
  val twoHours: BinanceCandleResolution = BinanceCandleResolution("2h", Duration.ofHours(2))
  val fourHours: BinanceCandleResolution = BinanceCandleResolution("4h", Duration.ofHours(4))
  val sixHours: BinanceCandleResolution = BinanceCandleResolution("6h", Duration.ofHours(6))
  val eightHours: BinanceCandleResolution = BinanceCandleResolution("8h", Duration.ofHours(8))
  val twelveHours: BinanceCandleResolution = BinanceCandleResolution("12h", Duration.ofHours(12))
  val oneDay: BinanceCandleResolution = BinanceCandleResolution("1d", Duration.ofDays(1))
  val threeDays: BinanceCandleResolution = BinanceCandleResolution("3d", Duration.ofDays(3))
  val oneWeek: BinanceCandleResolution = BinanceCandleResolution("1w", Duration.ofDays(7))
  // not used because one month is not a fix length
  //val oneMonth = BinanceCandleResolution("1M")

  val all = Set(
    oneMinute,
    threeMinutes,
    fiveMinutes,
    fifteenMinutes,
    thirtyMinutes,
    oneHour,
    twoHours,
    fourHours,
    sixHours,
    eightHours,
    twelveHours,
    oneDay,
    threeDays,
    oneWeek,
  )

  def forDuration(d: Duration): BinanceCandleResolution = all.find(_.candleLength == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported candle resolution: $d")
  }

}

case class BinanceCandleResolution(code: String, candleLength: Duration)
