package io.liquirium.connect.poloniex

import java.time.Duration

object PoloniexCandleLength {

  val oneMinute: PoloniexCandleLength = PoloniexCandleLength("MINUTE_1", Duration.ofMinutes(1))
  val fiveMinutes: PoloniexCandleLength = PoloniexCandleLength("MINUTE_5", Duration.ofMinutes(5))
  val tenMinutes: PoloniexCandleLength = PoloniexCandleLength("MINUTE_10", Duration.ofMinutes(10))
  val fifteenMinutes: PoloniexCandleLength = PoloniexCandleLength("MINUTE_15", Duration.ofMinutes(15))
  val thirtyMinutes: PoloniexCandleLength = PoloniexCandleLength("MINUTE_30", Duration.ofMinutes(30))
  val oneHour: PoloniexCandleLength = PoloniexCandleLength("HOUR_1", Duration.ofHours(1))
  val twoHours: PoloniexCandleLength = PoloniexCandleLength("HOUR_2", Duration.ofHours(2))
  val fourHours: PoloniexCandleLength = PoloniexCandleLength("HOUR_4", Duration.ofHours(4))
  val sixHours: PoloniexCandleLength = PoloniexCandleLength("HOUR_6", Duration.ofHours(6))
  val twelveHours: PoloniexCandleLength = PoloniexCandleLength("HOUR_12", Duration.ofHours(12))
  val oneDay: PoloniexCandleLength = PoloniexCandleLength("DAY_1", Duration.ofDays(1))
  val threeDays: PoloniexCandleLength = PoloniexCandleLength("DAY_3", Duration.ofDays(3))
  val oneWeek: PoloniexCandleLength = PoloniexCandleLength("WEEK_1", Duration.ofDays(7))
  // MONTH_1 not used because one month is not a fix length

  val all: Set[PoloniexCandleLength] = Set(
    oneMinute,
    fiveMinutes,
    tenMinutes,
    fifteenMinutes,
    thirtyMinutes,
    oneHour,
    twoHours,
    fourHours,
    sixHours,
    twelveHours,
    oneDay,
    threeDays,
    oneWeek
  )

  def forDuration(d: Duration): PoloniexCandleLength = all.find(_.length == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported Poloniex candle length: $d")
  }

  def forCode(code: String): PoloniexCandleLength = all.find(_.code == code) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported Poloniex candle code: $code")
  }

}

case class PoloniexCandleLength(code: String, length: Duration)

