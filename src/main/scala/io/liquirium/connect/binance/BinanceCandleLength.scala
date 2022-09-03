package io.liquirium.connect.binance

import java.time.Duration

object BinanceCandleLength {

  val oneMinute: BinanceCandleLength = BinanceCandleLength("1m", Duration.ofMinutes(1))
  val threeMinutes: BinanceCandleLength = BinanceCandleLength("3m", Duration.ofMinutes(3))
  val fiveMinutes: BinanceCandleLength = BinanceCandleLength("5m", Duration.ofMinutes(5))
  val fifteenMinutes: BinanceCandleLength = BinanceCandleLength("15m", Duration.ofMinutes(15))
  val thirtyMinutes: BinanceCandleLength = BinanceCandleLength("30m", Duration.ofMinutes(30))
  val oneHour: BinanceCandleLength = BinanceCandleLength("1h", Duration.ofHours(1))
  val twoHours: BinanceCandleLength = BinanceCandleLength("2h", Duration.ofHours(2))
  val fourHours: BinanceCandleLength = BinanceCandleLength("4h", Duration.ofHours(4))
  val sixHours: BinanceCandleLength = BinanceCandleLength("6h", Duration.ofHours(6))
  val eightHours: BinanceCandleLength = BinanceCandleLength("8h", Duration.ofHours(8))
  val twelveHours: BinanceCandleLength = BinanceCandleLength("12h", Duration.ofHours(12))
  val oneDay: BinanceCandleLength = BinanceCandleLength("1d", Duration.ofDays(1))
  val threeDays: BinanceCandleLength = BinanceCandleLength("3d", Duration.ofDays(3))
  val oneWeek: BinanceCandleLength = BinanceCandleLength("1w", Duration.ofDays(7))
  // not used because one month is not a fix length
  //val oneMonth = BinanceCandleLength("1M")

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

  def forDuration(d: Duration): BinanceCandleLength = all.find(_.candleLength == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported candle length: $d")
  }

}

case class BinanceCandleLength(code: String, candleLength: Duration)
