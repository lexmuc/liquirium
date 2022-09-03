package io.liquirium.bot

import io.liquirium.bot.BotInput.BotOutputHistory
import io.liquirium.bot.helpers.OperationRequestHelpers.{cancelRequestMessage, orderRequestMessage}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.OperationIntentHelpers.orderIntent
import io.liquirium.core.{BotId, CompoundTradeRequestId, Market, OrderConstraints, OperationIntent, OrderQuantityPrecision, PricePrecision}
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import io.liquirium.eval.{IncrementalContext, IncrementalSeq, InputUpdate, UpdatableContext}

import java.time.{Duration, Instant}

class SingleMarketBotTest extends BasicTest {

  private var startTime: Instant = _
  private var market: Market = MarketHelpers.market(1)
  private var orderConstraints: OrderConstraints =
    OrderConstraints(
      pricePrecision = PricePrecision.Infinite,
      orderQuantityPrecision = OrderQuantityPrecision.Infinite,
    )
  private var initialBaseBalance: BigDecimal = BigDecimal(0)
  private var initialQuoteBalance: BigDecimal = BigDecimal(0)
  private var candleLength: Duration = Duration.ofSeconds(1)
  private var minimumCandleHistoryLength = Duration.ofSeconds(0)
  private var getOrderIntents: SingleMarketBot.State => Seq[OrderIntent] = (_) => Seq()

  private var context: UpdatableContext = IncrementalContext()

  def makeBot(): SingleMarketBot = new SingleMarketBot {

    override def market: Market = SingleMarketBotTest.this.market

    override def orderConstraints: OrderConstraints = SingleMarketBotTest.this.orderConstraints

    override def startTime: Instant = SingleMarketBotTest.this.startTime

    override def initialBaseBalance: BigDecimal = SingleMarketBotTest.this.initialBaseBalance

    override def initialQuoteBalance: BigDecimal = SingleMarketBotTest.this.initialQuoteBalance

    override def candleLength: Duration = SingleMarketBotTest.this.candleLength

    override def minimumCandleHistoryLength: Duration = SingleMarketBotTest.this.minimumCandleHistoryLength

    override protected def getOrderIntents(state: SingleMarketBot.State): Seq[OperationIntent.OrderIntent] =
      SingleMarketBotTest.this.getOrderIntents(state)

  }

  private def fakePreviousOutputs(oo: BotOutput*): Unit = {
    context = context.update(InputUpdate(Map(BotOutputHistory -> IncrementalSeq.from(oo))))
  }

  private def requestId(n: Int) = CompoundTradeRequestId(BotId(""), n)

  def evaluate(): Seq[BotOutput] = {
    val (output, newContext) = context.evaluate(makeBot().eval)
    context = newContext
    output.get.toSeq
  }

  test("order intents without present orders are converted to trade request messages") {
    fakePreviousOutputs()
    getOrderIntents = _ => Seq(
      orderIntent("3", at = "5"),
      orderIntent("-2", at = "6"),
    )
    evaluate() shouldEqual Seq(
      orderRequestMessage(requestId(1), market, dec(3), price = dec(5)),
      orderRequestMessage(requestId(2), market, dec(-2), price = dec(6)),
    )
  }

  test("old outputs are taken into account for request numbering") {
    fakePreviousOutputs(
      cancelRequestMessage(requestId(1), market, orderId = "some_order_id"),
    )
    getOrderIntents = _ => Seq(
      orderIntent("-2", at = "6"),
    )
    evaluate() shouldEqual Seq(
      orderRequestMessage(requestId(2), market, dec(-2), price = dec(6)),
    )
  }

}
