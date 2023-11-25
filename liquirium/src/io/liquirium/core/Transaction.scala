package io.liquirium.core

object Transaction {

  case class Effect(ledger: LedgerRef, change: BigDecimal)

}

trait Transaction {

  def effects: Traversable[Transaction.Effect]

}
