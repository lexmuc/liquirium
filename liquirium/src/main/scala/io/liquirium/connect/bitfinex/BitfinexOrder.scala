package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexOrder.{OrderStatus, OrderType}
import io.liquirium.core.HistoryEntry

import java.time.Instant

object BitfinexOrder {
  sealed trait OrderType

  object OrderType {
    case object Limit extends OrderType

    case object ExchangeLimit extends OrderType

    case object Market extends OrderType

    case object ExchangeMarket extends OrderType

    case object Stop extends OrderType

    case object ExchangeStop extends OrderType

    case object StopLimit extends OrderType

    case object ExchangeStopLimit extends OrderType

    case object TrailingStop extends OrderType

    case object ExchangeTrailingStop extends OrderType

    case object Fok extends OrderType

    case object ExchangeFok extends OrderType

    case object Ioc extends OrderType

    case object ExchangeIoc extends OrderType
  }

  sealed trait OrderStatus

  object OrderStatus {
    case object Active extends OrderStatus
    case object Executed extends OrderStatus
    case object PartiallyFilled extends OrderStatus
    case object Canceled extends OrderStatus
    case object PostOnlyCanceled extends OrderStatus
  }

}

case class BitfinexOrder
(
  id: Long,
  clientOrderId: Long,
  symbol: String,
  creationTimestamp: Instant,
  updateTimestamp: Instant,
  amount: BigDecimal,
  originalAmount: BigDecimal,
  `type`: OrderType,
  status: OrderStatus,
  price: BigDecimal
) extends HistoryEntry {

  override def historyId: String = id.toString

  override def historyTimestamp: Instant = updateTimestamp

}
