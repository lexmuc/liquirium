package io.liquirium.core.helpers

import io.liquirium.core.LedgerRef
import io.liquirium.core.helpers.MarketHelpers.eid

object FeeHelper {

  def fees(n: Int): Seq[(LedgerRef, BigDecimal)] = Seq(LedgerRef(eid(n), n.toString) -> BigDecimal(n) / 10)

}
