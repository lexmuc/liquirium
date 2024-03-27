package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.bot.helpers.BotHelpers.botOutput
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.helpers.CandleHelpers.{c10, c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.core.helpers.OperationIntentHelpers.orderIntent
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import io.liquirium.core.{CandleHistorySegment, ExactResources, Market, Trade}
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.{Eval, IncrementalContext, InputEval, UpdatableContext}
import io.liquirium.util.TimePeriod
import org.scalatest.Matchers

import java.time.{Duration, Instant}

class SingleMarketStrategyBotTest extends BasicTest with Matchers {

  private var startTime: Instant = Instant.ofEpochSecond(0)
  private var endTime: Instant = Instant.ofEpochSecond(1000)
  private val market: Market = MarketHelpers.market(1)
  private var initialPrice: BigDecimal = BigDecimal(1)
  private var initialBaseBalance: BigDecimal = BigDecimal(0)
  private var initialQuoteBalance: BigDecimal = BigDecimal(0)
  private var candleLength: Duration = Duration.ofSeconds(1)
  private var minimumCandleHistoryLength = Duration.ofSeconds(0)
  private var strategyFunction: SingleMarketStrategy.State => Seq[OrderIntent] =
    (_: SingleMarketStrategy.State) => Seq()
  private var outputsByOrderIntents: Map[Seq[OrderIntent], Seq[BotOutput]] = Map(Seq() -> Seq())

  private var context: UpdatableContext = IncrementalContext()

  private def makeStrategy() = new SingleMarketStrategy {

    override def apply(state: SingleMarketStrategy.State): Seq[OrderIntent] =
      SingleMarketStrategyBotTest.this.strategyFunction(state)

    override def minimumCandleHistoryLength: Duration = SingleMarketStrategyBotTest.this.minimumCandleHistoryLength

    override def candleLength: Duration = SingleMarketStrategyBotTest.this.candleLength

    // the calculation of initial resources is not relevant for this test
    override def initialResources(totalQuoteValue: BigDecimal, initialPrice: BigDecimal): ExactResources = ???

  }

  def makeBot(): SingleMarketStrategyBot = SingleMarketStrategyBot(
    strategy = makeStrategy(),
    runConfiguration = SingleMarketStrategyBotRunConfiguration(
      market = market,
      operationPeriod = TimePeriod(startTime, endTime),
      initialPrice = initialPrice,
      initialResources = ExactResources(
        baseBalance = SingleMarketStrategyBotTest.this.initialBaseBalance,
        quoteBalance = SingleMarketStrategyBotTest.this.initialQuoteBalance,
      ),
    ),
    orderIntentConveyorEval = Eval.unit(
      (intents: Seq[OrderIntent]) => outputsByOrderIntents(intents)
    ),
  )

  private def fakeOrderIntentConversion(intents: OrderIntent*)(outputs: BotOutput*): Unit = {
    outputsByOrderIntents = outputsByOrderIntents.updated(intents, outputs)
  }

  private def fakeCandleHistory(input: CandleHistoryInput, chs: CandleHistorySegment): Unit = {
    context = context.update(inputUpdate(input -> chs))
  }

  private def fakeTradeHistory(start: Instant)(trades: Trade*): Unit = {
    val input = TradeHistoryInput(market, start)
    context = context.update(inputUpdate(input -> tradeHistorySegment(start)(trades: _*)))
  }

  private def fakeDefaultTradeHistory(): Unit = {
    val input = TradeHistoryInput(market, startTime)
    context = context.update(inputUpdate(input -> tradeHistorySegment(startTime)()))
  }

  private def fakeDefaultCandleHistory(): Unit =
    fakeCandleHistory(
      CandleHistoryInput(market, candleLength, startTime),
      candleHistorySegment(startTime, candleLength)(),
    )

  def evaluate(): Seq[BotOutput] = {
    val (output, newContext) = context.evaluate(makeBot().eval)
    context = newContext
    output.get.toSeq
  }

  private def assertState(
    p: SingleMarketStrategy.State => Boolean,
    getMessage: SingleMarketStrategy.State => String,
  ): Unit = {
    var state: SingleMarketStrategy.State = null
    fakeOrderIntentConversion(orderIntent(1))(botOutput(1))
    strategyFunction = s => {
      state = s
      if (p(s)) Seq(orderIntent(1)) else Seq()
    }
    if (evaluate() != Seq(botOutput(1))) fail(getMessage(state))
  }

  test("the order intents are passed to the conveyor in order to obtain bot outputs") {
    fakeDefaultCandleHistory()
    fakeDefaultTradeHistory()
    fakeOrderIntentConversion(
      orderIntent(1),
      orderIntent(2),
    )(
      botOutput(1),
      botOutput(2),
    )
    strategyFunction = _ => Seq(orderIntent(1), orderIntent(2))
    evaluate() shouldEqual Seq(botOutput(1), botOutput(2))
  }

  test("the candle history eval is made available as a field") {
    candleLength = secs(5)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(100)
    makeBot().candleHistoryEval shouldEqual InputEval(
      CandleHistoryInput(market, candleLength = secs(5), start = sec(90))
    )
  }

  test("the candle history is provided according to the market, candleLength and minimum length") {
    candleLength = secs(5)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(100)
    fakeDefaultTradeHistory()
    val chs = candleHistorySegment(
      c5(sec(90), 1),
      c5(sec(95), 2),
      c5(sec(100), 3),
    )
    fakeCandleHistory(CandleHistoryInput(market, candleLength = secs(5), start = sec(90)), chs)
    assertState(_.candleHistory == chs, _ => "candle history was not as expected")
  }

  test("the time in the bot state is the latest candle end time") {
    candleLength = secs(10)
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    fakeDefaultTradeHistory()
    val chs = candleHistorySegment(c10(sec(0), 1), c10(sec(10), 1), c10(sec(20), 1))
    fakeCandleHistory(CandleHistoryInput(market, candleLength = secs(10), start = sec(0)), chs)
    assertState(_.time == sec(30), s => s"time should be 30s but was ${s.time}")
  }

  test("the balances reflect the start balances plus the sum of all trade effects") {
    initialBaseBalance = dec(1)
    initialQuoteBalance = dec(10)
    startTime = sec(10)
    fakeDefaultCandleHistory()
    fakeTradeHistory(sec(10))(
      trade(
        id = "t1",
        market = market,
        time = sec(11),
        quantity = dec(2),
        price = dec(2),
        fees = Seq(market.quoteLedger -> dec(1))
      ),
      trade(
        id = "t2",
        market = market,
        time = sec(12),
        quantity = dec(-1),
        price = dec(5),
        fees = Seq(market.quoteLedger -> dec(1))
      ),
    )
    assertState(
      s => s.baseBalance == dec(2),
      s => s"base balance was ${s.baseBalance} but was expected to be 2",
    )
    assertState(
      s => s.quoteBalance == dec(9),
      s => s"quote balance was ${s.quoteBalance} but was expected to be 9",
    )
  }

  test("the given run configuration is part of the state") {
    initialBaseBalance = dec(10)
    initialQuoteBalance = dec(20)
    initialPrice = dec(123)
    candleLength = secs(10)
    startTime = sec(10)
    endTime = sec(110)
    fakeDefaultTradeHistory()
    fakeDefaultCandleHistory()
    val expectedRunConfiguration = SingleMarketStrategyBotRunConfiguration(
      market = market,
      operationPeriod = TimePeriod(startTime, endTime),
      initialPrice = initialPrice,
      initialResources = ExactResources(
        baseBalance = initialBaseBalance,
        quoteBalance = initialQuoteBalance,
      ),
    )
    assertState(
      _.runConfiguration == expectedRunConfiguration,
      s => s"run configuration was not as expected. got ${s.runConfiguration}",
    )
  }

  test("the balances are exposed as evals as well") {
    initialBaseBalance = dec(1)
    initialQuoteBalance = dec(10)
    startTime = sec(10)
    fakeDefaultCandleHistory()
    fakeTradeHistory(sec(10))(
      trade(
        id = "t1",
        market = market,
        time = sec(11),
        quantity = dec(2),
        price = dec(2),
        fees = Seq(market.quoteLedger -> dec(1))
      ),
      trade(
        id = "t2",
        market = market,
        time = sec(12),
        quantity = dec(-1),
        price = dec(5),
        fees = Seq(market.quoteLedger -> dec(1))
      ),
    )
    val (baseBalance, _) = context.evaluate(makeBot().baseBalanceEval)
    baseBalance.get shouldEqual dec(2)
    val (quoteBalance, _) = context.evaluate(makeBot().quoteBalanceEval)
    quoteBalance.get shouldEqual dec(9)
  }

  test("it exposes the trade history eval with correct market and start") {
    startTime = sec(100)
    makeBot().tradeHistoryEval shouldEqual InputEval(TradeHistoryInput(market, sec(100)))
  }

  test("before the bot start the no order intents are passed to the conveyor") {
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    fakeCandleHistory(
      CandleHistoryInput(market, candleLength, sec(0)),
      candleHistorySegment(sec(0), candleLength)(),
    )
    strategyFunction = _ => Seq(orderIntent(1), orderIntent(2))
    fakeDefaultTradeHistory()
    fakeOrderIntentConversion()(botOutput(1))
    evaluate() shouldEqual Seq(botOutput(1))
  }

  test("from the end on no order intents are passed to the conveyor") {
    minimumCandleHistoryLength = secs(10)
    startTime = sec(10)
    endTime = sec(30)
    fakeDefaultTradeHistory()
    strategyFunction = _ => Seq(orderIntent(1), orderIntent(2))
    fakeCandleHistory(
      CandleHistoryInput(market, candleLength, sec(0)),
      candleHistorySegment(sec(0), candleLength)(1, 2, 3),
    )
    fakeOrderIntentConversion()(botOutput(1))
    evaluate() shouldEqual Seq(botOutput(1))
    fakeCandleHistory(
      CandleHistoryInput(market, candleLength, sec(0)),
      candleHistorySegment(sec(0), candleLength)(1, 2, 3, 4),
    )
    evaluate() shouldEqual Seq(botOutput(1))
  }

}
