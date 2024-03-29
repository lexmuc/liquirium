package io.liquirium.core

import io.liquirium.core.Trade.Fees

import java.time.Instant

object Trade {

  type Fees = Seq[(LedgerRef, BigDecimal)]

}

final case class Trade(
  id: TradeId,
  market: Market,
  orderId: Option[String],
  quantity: BigDecimal,
  price: BigDecimal,
  fees: Fees,
  time: Instant,
) extends HistoryEntry with Transaction {

  override def historyId: String = id.toString

  override def historyTimestamp: Instant = time

  private val feesMap: Map[LedgerRef, BigDecimal] = fees.toMap

  lazy val effects: Iterable[Transaction.Effect] = {
    val baseAndQuoteEffects = Seq(
      market.baseLedger -> (quantity - feesMap.getOrElse(market.baseLedger, BigDecimal(0))),
      market.quoteLedger -> (-quantity * price - feesMap.getOrElse(market.quoteLedger, BigDecimal(0)))
    )
    (baseAndQuoteEffects ++ (feesMap - market.quoteLedger - market.baseLedger).transform((_, x) => x * -1))
      .map { case (k, v) => Transaction.Effect(k, v) }
  }

  def volume: BigDecimal = quantity.abs * price

  def isBuy: Boolean = quantity.signum > 0

  def isSell: Boolean = !isBuy

}