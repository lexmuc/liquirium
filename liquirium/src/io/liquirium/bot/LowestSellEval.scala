package io.liquirium.bot

import io.liquirium.bot.BotInput.OrderSnapshotHistoryInput
import io.liquirium.core.Market
import io.liquirium.eval.{CaseEval, Eval, InputEval}

case class LowestSellEval(market: Market, fallback: Eval[BigDecimal]) extends CaseEval[BigDecimal] {

  override protected def baseEval: Eval[BigDecimal] =
    Eval.map2(
      InputEval(OrderSnapshotHistoryInput(market)).map(_.lastSnapshot.orders),
      fallback,
    ) {
      case (openOrders, fb) =>
        openOrders.filter(_.isSell).toSeq.sortBy(_.price).headOption.map(_.price).getOrElse(fb)
    }

}
