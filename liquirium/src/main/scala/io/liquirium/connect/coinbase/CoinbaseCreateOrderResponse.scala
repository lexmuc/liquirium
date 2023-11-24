package io.liquirium.connect.coinbase

sealed trait CoinbaseCreateOrderResponse

object CoinbaseCreateOrderResponse {

  case class Success(
    orderId: String,
    clientOrderId: String,
  ) extends CoinbaseCreateOrderResponse


  case class Failure(
    error: String,
    message: String,
    details: String,
  ) extends CoinbaseCreateOrderResponse

}

