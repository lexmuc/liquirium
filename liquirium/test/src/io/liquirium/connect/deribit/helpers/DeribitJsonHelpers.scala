package io.liquirium.connect.deribit.helpers

import play.api.libs.json.{JsValue, Json}

object DeribitJsonHelpers {

  def deribitJsonOrder(
    orderId: String = "",
    direction: String = "buy",
    price: BigDecimal = BigDecimal(0),
    amount: BigDecimal = BigDecimal(0),
    filledQuantity: BigDecimal = BigDecimal(0),
    instrumentName: String = "",
    orderState: String = "open",
  ): JsValue =
    Json.parse(
      s"""{
        "order_id": "$orderId",
        "direction": "$direction",
        "price": $price,
        "amount": $amount,
        "filled_amount": $filledQuantity,
        "instrument_name": "$instrumentName",
        "order_state": "$orderState"
    }""")

  def deribitTradeJson(
    id: String = "",
    sequenceNumber: Long = 0,
    direction: String = "buy",
    orderId: String = "",
    instrument: String = "",
    quantity: BigDecimal = BigDecimal(0),
    price: BigDecimal = BigDecimal(0),
    indexPrice: BigDecimal = BigDecimal(0),
    fee: BigDecimal = BigDecimal(0),
    feeCurrency: String = "",
    timestamp: Long = 0
  ): JsValue = Json.parse(
    s"""{
       |  "trade_seq": $sequenceNumber,
       |  "trade_id": "$id",
       |  "timestamp": $timestamp,
       |  "tick_direction": 1,
       |  "state": "open",
       |  "self_trade": false,
       |  "reduce_only": false,
       |  "price": $price,
       |  "post_only": false,
       |  "order_type": "limit",
       |  "order_id": "$orderId",
       |  "matching_id": null,
       |  "liquidity": "M",
       |  "iv": 56.83,
       |  "instrument_name": "$instrument",
       |  "index_price": $indexPrice,
       |  "fee_currency": "$feeCurrency",
       |  "fee": $fee,
       |  "direction": "$direction",
       |  "amount": $quantity
       |}""".stripMargin
  )

}
