package io.liquirium.connect.deribit.helpers

import io.liquirium.connect.deribit.DeribitError.{AuthenticationError, OtherApiError}
import io.liquirium.connect.deribit._
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import play.api.libs.json.JsNumber

import java.time.Instant

object DeribitTestHelpers {

  def deribitOrder(
    id: String = "999",
    instrument: String = "ABCDEF",
    direction: DeribitDirection = DeribitDirection.Buy,
    quantity: BigDecimal = dec("45.123"),
    price: BigDecimal = dec("123.45"),
    filledQuantity: BigDecimal = dec("0.02"),
    state: DeribitOrder.State = DeribitOrder.State.Open,
  ): DeribitOrder =
    DeribitOrder(id = id,
      direction = direction,
      price = price,
      quantity = quantity,
      instrument = instrument,
      state = state,
      filledQuantity = filledQuantity)

  def deribitCandle(
    time: Instant = sec(0),
    open: BigDecimal = BigDecimal(1),
    close: BigDecimal = BigDecimal(1),
    high: BigDecimal = BigDecimal(1),
    low: BigDecimal = BigDecimal(1),
    volume: BigDecimal = BigDecimal(0),
    cost: BigDecimal = BigDecimal(0),
  ): DeribitCandle =
    DeribitCandle(
      tick = time,
      open = open,
      close = close,
      high = high,
      low = low,
      volume = volume,
      cost = cost
    )

  def deribitTrade(
    id: String = "123",
    sequenceNumber: Long = 0,
    direction: DeribitDirection = DeribitDirection.Buy,
    orderId: String = "987",
    instrument: String = "ABCDEF",
    quantity: BigDecimal = BigDecimal("1.23"),
    price: BigDecimal = BigDecimal("2.34"),
    fee: BigDecimal = BigDecimal("0.12"),
    feeCurrency: String = "XYZ",
    timestamp: Long = 12345678,
  ): DeribitTrade =
    DeribitTrade(
      id = id,
      sequenceNumber = sequenceNumber,
      direction = direction,
      orderId = orderId,
      instrument = instrument,
      quantity = quantity,
      price = price,
      indexPrice = BigDecimal(0),
      fee = fee,
      feeCurrency = feeCurrency,
      timestamp = timestamp)

  def trade(n: Int): DeribitTrade = deribitTrade(n.toString)

  def authError(n: Int): AuthenticationError = AuthenticationError(n.toString, Some(JsNumber(n)))

  def error(n: Int): OtherApiError = OtherApiError(n, n.toString, None)

  def continuationToken(s: String): DeribitContinuationToken = DeribitContinuationToken(s)

}
