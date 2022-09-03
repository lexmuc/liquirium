package io.liquirium.connect.coinbase

case class CoinbaseCancelOrderResult(
  success: Boolean,
  failureReason: String,
  orderId: String
)


