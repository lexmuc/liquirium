package io.liquirium.bot.simulation

import io.liquirium.core.{LedgerRef, Market}
import io.liquirium.eval.Eval

import java.time.Instant

trait VisualizationMetric {

  def getEval(market: Market, startTime: Instant): Eval[BigDecimal]

}


object VisualizationMetric {

  //noinspection ConvertExpressionToSAM
  def marketIndependentMetric(eval: Eval[BigDecimal]): VisualizationMetric =
    new VisualizationMetric {
      override def getEval(market: Market, startTime: Instant): Eval[BigDecimal] = eval
    }

  //noinspection ConvertExpressionToSAM
  def baseBalanceMetric(
    tradeMarkets: Iterable[Market],
    initialBalances: Map[LedgerRef, BigDecimal],
  ): VisualizationMetric =
    new VisualizationMetric {
      override def getEval(market: Market, startTime: Instant): Eval[BigDecimal] =
        BalanceEval(
          ledgerRef = market.baseLedger,
          tradeMarkets = tradeMarkets,
          startTime = startTime,
          initialBalance = initialBalances(market.baseLedger),
        )
    }

  //noinspection ConvertExpressionToSAM
  def quoteBalanceMetric(
    tradeMarkets: Iterable[Market],
    initialBalances: Map[LedgerRef, BigDecimal],
  ): VisualizationMetric =
    new VisualizationMetric {
      override def getEval(market: Market, startTime: Instant): Eval[BigDecimal] =
        BalanceEval(
          ledgerRef = market.quoteLedger,
          tradeMarkets = tradeMarkets,
          startTime = startTime,
          initialBalance = initialBalances(market.quoteLedger),
        )
    }

}
