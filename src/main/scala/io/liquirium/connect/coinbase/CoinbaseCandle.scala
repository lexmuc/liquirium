package io.liquirium.connect.coinbase

import java.time.Instant

/**
 * @param start  Timestamp for bucket start time, in UNIX time.
 * @param low    Lowest price during the bucket interval.
 * @param high   Highest price during the bucket interval.
 * @param open   Opening price (first trade) in the bucket interval.
 * @param close  Closing price (last trade) in the bucket interval.
 * @param volume Volume of trading activity during the bucket interval.
 */

case class CoinbaseCandle(
  start: Instant,
  low: BigDecimal,
  high: BigDecimal,
  open: BigDecimal,
  close: BigDecimal,
  volume: BigDecimal,
)
