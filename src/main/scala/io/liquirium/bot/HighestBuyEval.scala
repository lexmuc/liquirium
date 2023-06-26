package io.liquirium.bot

import io.liquirium.bot.BotInput.OrderSnapshotHistoryInput
import io.liquirium.core.Market
import io.liquirium.eval.{Eval, InputEval}

object HighestBuyEval {

  def apply(market: Market, fallback: Eval[BigDecimal]): Eval[BigDecimal] =
    Eval.map2(
      InputEval(OrderSnapshotHistoryInput(market)).map(_.lastSnapshot.orders),
      fallback,
    ) {
      case (openOrders, fb) =>
        openOrders.filter(_.isBuy).toSeq.sortBy(_.price * -1).headOption.map(_.price).getOrElse(fb)
    }

}
