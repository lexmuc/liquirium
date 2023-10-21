package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.TradeHistoryInput
import io.liquirium.core.{LedgerRef, Market, Trade}
import io.liquirium.core.Trade.Fees
import io.liquirium.core.helpers.CoreHelpers.{dec, exchangeId, ledgerRef, sec}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.eval.helpers.EvalTest

class BalanceEvalTest extends EvalTest {
  private val startTime = sec(100)
  private val eid = exchangeId("eid")
  private val btcUsd = MarketHelpers.market(eid, "BTC", "USD")
  private val ethBtc = MarketHelpers.market(eid, "ETH", "BTC")
  private val ethUsd = MarketHelpers.market(eid, "ETH", "USD")
  private val btcLedger = ledgerRef(eid, "BTC")
  private val ethLedger = ledgerRef(eid, "ETH")
  private val usdLedger = ledgerRef(eid, "USD")

  private def btcUsdTrade(n: Int, quantity: String, at: String, fees: Fees = Seq()) =
    trade(
      id = "btcusd" + n.toString,
      market = btcUsd,
      quantity = dec(quantity),
      price = dec(at),
      time = sec(100 + n),
      fees = fees,
    )

  private def ethBtcTrade(n: Int, quantity: String, at: String, fees: Fees = Seq()) =
    trade(
      id = "ethbtc" + n.toString,
      market = ethBtc,
      quantity = dec(quantity),
      price = dec(at),
      time = sec(100 + n),
      fees = fees,
    )

  private def ethUsdTrade(n: Int, quantity: String, at: String, fees: Fees = Seq()) =
    trade(
      id = "ethusd" + n.toString,
      market = ethUsd,
      quantity = dec(quantity),
      price = dec(at),
      time = sec(100 + n),
      fees = fees,
    )

  private def fakeTrades(market: Market)(trades: Trade*): Unit =
    fakeInput(TradeHistoryInput(market, startTime), tradeHistorySegment(startTime)(trades: _*))

  private def balanceEval(ledgerRef: LedgerRef, tradeMarkets: Seq[Market], initialBalance: String) =
    BalanceEval(
      ledgerRef = ledgerRef,
      startTime = startTime,
      tradeMarkets = tradeMarkets,
      initialBalance = dec(initialBalance),
    )

  test("it evaluates to the correct balance for the given ledger taking into account all relevant markets") {
    fakeTrades(btcUsd)(
      btcUsdTrade(1, quantity = "0.1", at = "10000"),
      btcUsdTrade(2, quantity = "-0.2", at = "20000"),
    )
    fakeTrades(ethBtc)(
      ethBtcTrade(3, quantity = "2", at = "0.1"),
    )
    fakeTrades(ethUsd)(
      ethUsdTrade(4, quantity = "1", at = "5000"),
    )

    val markets = Seq(btcUsd, ethBtc, ethUsd)
    evaluate(balanceEval(btcLedger, markets, "1.0")).get shouldEqual dec("0.7")
    evaluate(balanceEval(ethLedger, markets, "10.0")).get shouldEqual dec("13.0")
    evaluate(balanceEval(usdLedger, markets, "10000")).get shouldEqual dec("8000")
  }

  test("fees are taken into account") {
    fakeTrades(btcUsd)(
      btcUsdTrade(1, quantity = "0.1", at = "10000", fees = Seq(
        (btcLedger, dec("0.01")),
        (usdLedger, dec("10")),
      )),
    )
    val markets = Seq(btcUsd)
    evaluate(balanceEval(btcLedger, markets, "1.0")).get shouldEqual dec("1.09")
    evaluate(balanceEval(usdLedger, markets, "10000.0")).get shouldEqual dec("8990")
  }

}
