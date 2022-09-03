package io.liquirium.core.helper

import io.liquirium.core.helper.CoreHelpers._
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.{Duration, Instant}

object CandleHelpers {

  case class OHLC
  (
    open: BigDecimal,
    high: BigDecimal,
    low: BigDecimal,
    close: BigDecimal
  )

  def ohlc(price: BigDecimal): OHLC = OHLC(price, price, price, price)

  def ohlc(n: Int): OHLC = ohlc(dec(n))

  def candle(start: Instant = sec(1),
             length: Duration = secs(1),
             open: BigDecimal = null,
             close: BigDecimal = null,
             high: BigDecimal = 1,
             low: BigDecimal = 1,
             quoteVolume: BigDecimal = 1): Candle =
    Candle(
      startTime = start,
      length = length,
      open = if (open == null) (high + low) / 2 else open,
      close = if (close == null) (high + low) / 2 else close,
      high = high,
      low = low,
      quoteVolume = quoteVolume
    )

  def ohlcCandle(start: Instant = sec(1),
                 length: Duration = secs(1),
                 ohlc: OHLC = this.ohlc(BigDecimal(1)),
                 quoteVolume: BigDecimal = BigDecimal(1)): Candle =
    Candle(
      startTime = start,
      length = length,
      open = ohlc.open,
      close = ohlc.close,
      high = ohlc.high,
      low = ohlc.low,
      quoteVolume = quoteVolume
    )

  def emptyCandle(start: Instant = sec(1), length: Duration = secs(1)): Candle = Candle.empty(start, length)

  def emptyCandle(n: Int): Candle = emptyCandle(start = sec(n))

  def candle(price: BigDecimal, volume: BigDecimal): Candle =
    candle(open = price, close = price, high = price, low = price, quoteVolume = volume)

  def candle(n: Int): Candle = candle(BigDecimal(n), volume = BigDecimal(1)).copy(startTime = sec(n))

  def candleHistorySegment(start: Instant, resolution: Duration)(candleData: Int*): CandleHistorySegment = {
    val emptySegment = CandleHistorySegment.empty(start, resolution)
    candleData.foldLeft(emptySegment) { (s, n) =>
      val nextCandle = ohlcCandle(s.end, s.resolution, ohlc(n))
      s.append(nextCandle)
    }
  }

}
