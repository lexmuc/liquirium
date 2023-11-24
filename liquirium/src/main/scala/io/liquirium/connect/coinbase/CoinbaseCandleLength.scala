package io.liquirium.connect.coinbase

import java.time.Duration

object CoinbaseCandleLength {

  val oneMinute: CoinbaseCandleLength = CoinbaseCandleLength("ONE_MINUTE", Duration.ofMinutes(1))
  val fiveMinutes: CoinbaseCandleLength = CoinbaseCandleLength("FIVE_MINUTE", Duration.ofMinutes(5))
  val fifteenMinutes: CoinbaseCandleLength = CoinbaseCandleLength("FIFTEEN_MINUTE", Duration.ofMinutes(15))
  val thirtyMinutes: CoinbaseCandleLength = CoinbaseCandleLength("THIRTY_MINUTE", Duration.ofMinutes(30))
  val oneHour: CoinbaseCandleLength = CoinbaseCandleLength("ONE_HOUR", Duration.ofHours(1))
  val twoHours: CoinbaseCandleLength = CoinbaseCandleLength("TWO_HOUR", Duration.ofHours(2))
  val sixHours: CoinbaseCandleLength = CoinbaseCandleLength("SIX_HOUR", Duration.ofHours(6))
  val oneDay: CoinbaseCandleLength = CoinbaseCandleLength("ONE_DAY", Duration.ofDays(1))

  val all: Set[CoinbaseCandleLength] = Set(
    oneMinute,
    fiveMinutes,
    fifteenMinutes,
    thirtyMinutes,
    oneHour,
    twoHours,
    sixHours,
    oneDay,
  )

  def forDuration(d: Duration): CoinbaseCandleLength = all.find(_.length == d) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported Coinbase candle length: $d")
  }

  def forCode(code: String): CoinbaseCandleLength = all.find(_.code == code) match {
    case Some(r) => r
    case None => throw new RuntimeException(s"Unsupported Coinbase candle code: $code")
  }

}

case class CoinbaseCandleLength(code: String, length: Duration)

