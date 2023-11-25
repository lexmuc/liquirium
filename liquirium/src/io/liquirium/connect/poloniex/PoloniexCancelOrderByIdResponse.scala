package io.liquirium.connect.poloniex

case class PoloniexCancelOrderByIdResponse(
  orderId: String,
  clientOrderId: String,
  state: String,
  code: Int,
  message: String,
)
