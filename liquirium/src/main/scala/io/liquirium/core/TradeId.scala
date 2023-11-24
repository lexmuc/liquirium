package io.liquirium.core

trait TradeId extends Ordered[TradeId]

case class StringTradeId(value: String) extends TradeId {

  override def toString: String = value

  override def compare(that: TradeId): Int = that match {
    case StringTradeId(otherValue) => this.value.compareTo(otherValue)
  }

}


