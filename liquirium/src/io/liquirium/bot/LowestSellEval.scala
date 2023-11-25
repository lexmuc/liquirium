package io.liquirium.bot

import io.liquirium.bot.BotInput.OrderSnapshotHistoryInput
import io.liquirium.core.Market
import io.liquirium.eval.{Eval, InputEval}

object LowestSellEval {

  def apply(market: Market, fallback: Eval[BigDecimal]): Eval[BigDecimal] =
    Eval.map2(
      InputEval(OrderSnapshotHistoryInput(market)).map(_.lastSnapshot.orders),
      fallback,
    ) {
      case (openOrders, fb) =>
        openOrders.filter(_.isSell).toSeq.sortBy(_.price).headOption.map(_.price).getOrElse(fb)
    }

}
