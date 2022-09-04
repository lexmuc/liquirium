package io.liquirium.core

import java.time.{Duration, Instant}

object Candle {

  def reverse(candles: Seq[Candle]): Seq[Candle] = candles.reverseMap(_.reverse)

  def empty(start: Instant, length: Duration): Candle = Candle(
    startTime = start,
    length = length,
    open = BigDecimal(0),
    high = BigDecimal(0),
    low = BigDecimal(0),
    close = BigDecimal(0),
    quoteVolume = BigDecimal(0)
  )

  def aggregate(candles: Iterable[Candle]): Candle = {
    val first = earliest(candles)
    val last = latest(candles)
    val startTime = first.startTime
    val length = Duration.between(first.startTime, last.endTime)
    val nonEmptyCandles = candles.filter(_.quoteVolume > BigDecimal(0))
    if (nonEmptyCandles.isEmpty) empty(startTime, length)
    else {
      Candle(
        startTime = startTime,
        length = length,
        open = earliest(nonEmptyCandles).open,
        close = latest(nonEmptyCandles).close,
        high = nonEmptyCandles.map(_.high).max,
        low = nonEmptyCandles.map(_.low).min,
        quoteVolume = nonEmptyCandles.map(_.quoteVolume).sum
      )
    }
  }

  def aggregateInInterval(start: Instant, length: Duration, candles: Iterable[Candle]): Candle =
    aggregate(candles).copy(startTime = start, length = length)

  private def earliest(candles: Iterable[Candle]) =
    candles.tail.foldLeft (candles.head) { (m, x) => if (x.startTime.isBefore(m.startTime)) x else m }

  private def latest(candles: Iterable[Candle]) =
    candles.tail.foldLeft(candles.head) { (m, x) => if (x.endTime.isAfter(m.endTime)) x else m }

}

case class Candle
(
  startTime: Instant,
  length: Duration,
  open: BigDecimal,
  close: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  quoteVolume: BigDecimal
) extends HistoryEntry {

  def mapPrice(f: BigDecimal => BigDecimal): Candle =
    copy(
      open = f(open),
      close = f(close),
      high = f(high).max(f(low)),
      low = f(high).min(f(low))
    )

  def scale(factor: Double): Candle = mapPrice(_ * factor)

  def reverse: Candle = copy(
    startTime = startTime plus length,
    open = close,
    close = open
  )

  def endTime: Instant = startTime plus length

  def isEmpty: Boolean = quoteVolume == BigDecimal(0)

  override def historyId: String = startTime.toEpochMilli.toString

  override def historyTimestamp: Instant = startTime
}