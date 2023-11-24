package io.liquirium.connect.deribit

object DeribitOrder {
  sealed trait State

  object State {
    object Open extends State

    object Filled extends State

    object Rejected extends State

    object Cancelled extends State

    object Untriggered extends State
  }
}

case class DeribitOrder(
  id: String,
  direction: DeribitDirection,
  price: BigDecimal,
  quantity: BigDecimal,
  filledQuantity: BigDecimal,
  instrument: String,
  state: DeribitOrder.State,
)
