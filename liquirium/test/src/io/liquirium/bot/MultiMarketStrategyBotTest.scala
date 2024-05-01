package io.liquirium.bot

import io.liquirium.bot
import io.liquirium.bot.BotInput.{CandleHistoryInput, TimeInput, TradeHistoryInput}
import io.liquirium.bot.helpers.BotHelpers.botOutput
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c10, c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers._
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.core.helpers.OperationIntentHelpers.orderIntent
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.{CandleHistorySegment, LedgerRef, Market, Trade}
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.{Eval, IncrementalContext, InputEval, UpdatableContext}
import io.liquirium.util.TimePeriod
import org.scalatest.Matchers

import java.time.{Duration, Instant}

class MultiMarketStrategyBotTest extends BasicTest with Matchers {

  private var startTime: Instant = Instant.ofEpochSecond(0)
  private var endTime: Instant = Instant.ofEpochSecond(1000)
  private var initialPricesByMarket: Map[Market, BigDecimal] = Map()
  private var initialBalancesByLedgerRef: Map[LedgerRef, BigDecimal] = Map()
  private var initialValue: BigDecimal = BigDecimal(1)
  private var candleLength: Duration = Duration.ofSeconds(1)
  private var minimumCandleHistoryLength = Duration.ofSeconds(0)
  private var strategyFunction: bot.MultiMarketStrategy.State => Map[Market, Seq[OrderIntent]] =
    (_: bot.MultiMarketStrategy.State) => Map()

  private var outputsByMarketAndOrderIntents: Map[(Market, Seq[OrderIntent]), Seq[BotOutput]] = Map()

  private var context: UpdatableContext = IncrementalContext()

  private def markets: Seq[Market] = initialPricesByMarket.keys.toSeq

  private def makeStrategy() = new MultiMarketStrategy {

    override def apply(state: bot.MultiMarketStrategy.State): Map[Market, Seq[OrderIntent]] =
      MultiMarketStrategyBotTest.this.strategyFunction(state)

    override def minimumCandleHistoryLength: Duration = MultiMarketStrategyBotTest.this.minimumCandleHistoryLength

    override def candleLength: Duration = MultiMarketStrategyBotTest.this.candleLength

    //noinspection NotImplementedCode
    // the calculation of initial balances is not relevant for this test
    override def calculateInitialBalances(
      totalQuoteValue: BigDecimal,
      initialPrices: Map[Market, BigDecimal],
    ): Map[LedgerRef, BigDecimal] = ???

  }

  def makeBot(): MultiMarketStrategyBot = MultiMarketStrategyBot(
    strategy = makeStrategy(),
    runConfiguration = MultiMarketStrategyBotRunConfiguration(
      operationPeriod = TimePeriod(startTime, endTime),
      initialPricesByMarket = initialPricesByMarket,
      initialBalances = initialBalancesByLedgerRef,
      initialValue = initialValue,
    ),
    orderIntentConveyorsByMarketEval = Eval.unit(
      markets.map { m =>
        m -> ((intents: Seq[OrderIntent]) => outputsByMarketAndOrderIntents((m, intents)))
      }.toMap
    )
  )

  private def fakeOrderIntentConversion(market: Market, intents: OrderIntent*)(outputs: BotOutput*): Unit = {
    outputsByMarketAndOrderIntents = outputsByMarketAndOrderIntents.updated((market, intents), outputs)
  }

  private def fakeCandleHistory(input: CandleHistoryInput, chs: CandleHistorySegment): Unit = {
    context = context.update(inputUpdate(input -> chs))
  }

  private def fakeMarkets(markets: Market*): Unit = {
    initialPricesByMarket = markets.map(_ -> BigDecimal(1)).toMap
    val ledgers = markets.foldLeft(Set[LedgerRef]())((s, m) => s + m.baseLedger + m.quoteLedger)
    initialBalancesByLedgerRef = ledgers.map(_ -> BigDecimal(0)).toMap
  }

  private def fakeDefaultTradeHistories(): Unit = {
    for (m <- markets) {
      context = context.update(inputUpdate(
        TradeHistoryInput(m, startTime) -> tradeHistorySegment(startTime)()
      ))
    }
  }

  private def fakeTradeHistory(m: Market)(trades: Trade*): Unit = {
    context = context.update(inputUpdate(
      TradeHistoryInput(m, startTime) -> tradeHistorySegment(startTime)(trades: _*)
    ))
  }

  private def fakeTime(candleLength: Duration, t: Instant): Unit = {
    context = context.update(inputUpdate(TimeInput(candleLength) -> t))
  }

  private def fakeDefaultTime(): Unit = {
    fakeTime(candleLength, startTime)
  }

  private def fakeDefaultCandleHistories(): Unit =
    for (m <- markets) {
      fakeCandleHistory(
        CandleHistoryInput(m, candleLength, startTime minus minimumCandleHistoryLength),
        candleHistorySegment(startTime minus minimumCandleHistoryLength, candleLength)(),
      )
    }

  def evaluate(): Seq[BotOutput] = {
    val (output, newContext) = context.evaluate(makeBot().eval)
    context = newContext
    output.get.toSeq
  }

  private def assertState(
    p: bot.MultiMarketStrategy.State => Boolean,
    getMessage: bot.MultiMarketStrategy.State => String,
  ): Unit = {
    var state: bot.MultiMarketStrategy.State = null
    for {
      m <- markets
    } {
      fakeOrderIntentConversion(m, orderIntent(1))(botOutput(1))
    }
    fakeOrderIntentConversion(markets.head, orderIntent(1))(botOutput(1))
    strategyFunction = s => {
      state = s
      if (p(s)) {
        markets.map(m => (m, Seq(orderIntent(1)))).toMap
      }
      else {
        markets.map(m => (m, Seq())).toMap
      }
    }
    if (evaluate() != Seq(botOutput(1))) fail(getMessage(state))
  }

  test("the order intents are passed to the respective conveyor in order to obtain bot outputs per market") {
    fakeMarkets(market(1), market(2))
    fakeDefaultCandleHistories()
    fakeDefaultTradeHistories()
    fakeDefaultTime()
    fakeOrderIntentConversion(market(1), orderIntent(1), orderIntent(2))(
      botOutput(1),
      botOutput(2),
    )
    fakeOrderIntentConversion(market(2), orderIntent(3))(
      botOutput(3),
    )
    strategyFunction = _ => Map(
      market(1) -> Seq(orderIntent(1), orderIntent(2)),
      market(2) -> Seq(orderIntent(3)),
    )
    evaluate() shouldEqual Seq(botOutput(1), botOutput(2), botOutput(3))
  }

  test("the candle history evals are made available by market") {
    fakeMarkets(market(1), market(2))
    candleLength = secs(5)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(100)
    makeBot().candleHistoryEvalsByMarket shouldEqual Map(
      market(1) -> InputEval(CandleHistoryInput(market(1), candleLength = secs(5), start = sec(90))),
      market(2) -> InputEval(CandleHistoryInput(market(2), candleLength = secs(5), start = sec(90))),
    )
  }

  test("the candle histories are provided according to the market, candleLength and minimum length") {
    candleLength = secs(5)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(100)
    fakeMarkets(market(1), market(2))
    fakeDefaultTradeHistories()
    fakeDefaultTime()
    val chs1 = candleHistorySegment(
      c5(sec(90), 1),
      c5(sec(95), 2),
    )
    val chs2 = candleHistorySegment(
      c5(sec(90), 2),
      c5(sec(95), 3),
    )
    fakeCandleHistory(CandleHistoryInput(market(1), candleLength = secs(5), start = sec(90)), chs1)
    fakeCandleHistory(CandleHistoryInput(market(2), candleLength = secs(5), start = sec(90)), chs2)
    assertState(_.candleHistoriesByMarket == Map(
      market(1) -> chs1,
      market(2) -> chs2,
    ), _ => "candle histories were not as expected")
  }

  test("the time in the bot state is the current time in candle resolution, not necessarily the latest candle end") {
    candleLength = secs(10)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    fakeMarkets(market(1))
    fakeDefaultTradeHistories()
    val chs = candleHistorySegment(c10(sec(0), 1), c10(sec(10), 1))
    fakeCandleHistory(CandleHistoryInput(market(1), candleLength = secs(10), start = sec(0)), chs)
    fakeTime(candleLength, sec(50))
    assertState(_.time == sec(50), s => s"time should be 50s but was ${s.time}")
  }

  test("the balances reflect the start balances plus the sum of all trade effects") {
    val eid = exchangeId(1)
    val baseLedgerA = ledgerRef(eid, "BASE_A")
    val baseLedgerB = ledgerRef(eid, "BASE_B")
    val quoteLedger = ledgerRef(eid, "QUOTE")
    val marketA = market(eid, "BASE_A", "QUOTE")
    val marketB = market(eid, "BASE_B", "QUOTE")

    fakeMarkets(marketA, marketB)
    initialBalancesByLedgerRef = Map(
      baseLedgerA -> dec(0),
      baseLedgerB -> dec(1),
      quoteLedger -> dec(10),
    )
    startTime = sec(10)
    fakeDefaultCandleHistories()
    fakeDefaultTime()

    val buy2AFor4 = trade(
      id = "buy2AFor4",
      market = marketA,
      time = sec(11),
      quantity = dec(2),
      price = dec(2),
    )

    val sell1BFor4 = trade(
      id = "sell1BFor4",
      market = marketB,
      time = sec(12),
      quantity = dec(-1),
      price = dec(4),
    )

    val buy2BFor6Fee1 = trade(
      id = "buy2BFor3Fee1",
      market = marketB,
      time = sec(13),
      quantity = dec(2),
      price = dec(3),
      fees = Seq(quoteLedger -> dec(1)),
    )

    fakeTradeHistory(marketA)(buy2AFor4)
    fakeTradeHistory(marketB)(sell1BFor4, buy2BFor6Fee1)

    assertState(
      s => s.balances(baseLedgerA) == dec(2),
      s => s"balance for baseLedgerA was ${s.balances(baseLedgerA)} but was expected to be 2",
    )
    assertState(
      s => s.balances(baseLedgerB) == dec(2),
      s => s"balance for baseLedgerB was ${s.balances(baseLedgerB)} but was expected to be 2",
    )
    assertState(
      s => s.balances(quoteLedger) == dec(3),
      s => s"balance for quoteLedger was ${s.balances(quoteLedger)} but was expected to be 3",
    )
  }

  test("the given run configuration is part of the state") {
    fakeMarkets(market(1))
    startTime = sec(10)
    endTime = sec(110)
    candleLength = secs(10)
    fakeDefaultTime()
    initialBalancesByLedgerRef = Map(
      market(1).baseLedger -> dec(10),
      market(1).quoteLedger -> dec(20),
    )
    initialPricesByMarket = Map(market(1) -> dec(123))
    initialValue = dec(1234)
    fakeDefaultTradeHistories()
    fakeDefaultCandleHistories()
    val expectedRunConfiguration = MultiMarketStrategyBotRunConfiguration(
      operationPeriod = TimePeriod(startTime, endTime),
      initialPricesByMarket = initialPricesByMarket,
      initialBalances = initialBalancesByLedgerRef,
      initialValue = dec(1234),
    )
    assertState(
      _.runConfiguration == expectedRunConfiguration,
      s => s"run configuration was not as expected. got ${s.runConfiguration}",
    )
  }

  test("the balances are exposed as evals as well") {
    val eid = exchangeId(1)
    val baseLedgerA = ledgerRef(eid, "BASE_A")
    val quoteLedger = ledgerRef(eid, "QUOTE")
    val marketA = market(eid, "BASE_A", "QUOTE")

    fakeMarkets(marketA)
    initialBalancesByLedgerRef = Map(
      baseLedgerA -> dec(1),
      quoteLedger -> dec(10),
    )
    startTime = sec(10)
    fakeDefaultCandleHistories()

    val buy2AFor4 = trade(
      id = "buy2AFor4",
      market = marketA,
      time = sec(11),
      quantity = dec(2),
      price = dec(2),
    )

    fakeTradeHistory(marketA)(buy2AFor4)

    val (baseBalance, _) = context.evaluate(makeBot().balanceEvalsByLedgerRef(baseLedgerA))
    baseBalance.get shouldEqual dec(3)
    val (quoteBalance, _) = context.evaluate(makeBot().balanceEvalsByLedgerRef(quoteLedger))
    quoteBalance.get shouldEqual dec(6)
  }

  test("it exposes the trade history eval with correct market and start") {
    fakeMarkets(market(1), market(2))
    startTime = sec(100)
    endTime = sec(200)
    makeBot().tradeHistoryEvalsByMarket shouldEqual Map(
      market(1) -> InputEval(TradeHistoryInput(market(1), sec(100))),
      market(2) -> InputEval(TradeHistoryInput(market(2), sec(100))),
    )
  }

  test("before the bot start no order intents are passed to the conveyor but outputs are generated") {
    fakeMarkets(market(1))
    candleLength = secs(10)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    fakeDefaultCandleHistories()
    fakeTime(candleLength, sec(0))
    strategyFunction = _ => Map(market(1) -> Seq(orderIntent(1), orderIntent(2)))
    fakeDefaultTradeHistories()
    fakeOrderIntentConversion(market(1))(botOutput(1))
    evaluate() shouldEqual Seq(botOutput(1))
  }

  test("from the end on no order intents are passed to the conveyor but outputs are generated") {
    fakeMarkets(market(1))
    candleLength = secs(10)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    endTime = sec(30)
    fakeDefaultTradeHistories()
    fakeDefaultCandleHistories()
    strategyFunction = _ => Map(market(1) -> Seq(orderIntent(1), orderIntent(2)))
    fakeTime(candleLength, sec(30))
    fakeOrderIntentConversion(market(1))(botOutput(1))
    evaluate() shouldEqual Seq(botOutput(1))
    fakeTime(candleLength, sec(40))
    evaluate() shouldEqual Seq(botOutput(1))
  }

}
