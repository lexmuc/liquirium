package io.liquirium.bot

import io.liquirium.bot.BotInput.{BotOutputHistory, CandleHistoryInput}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.{BotId, CandleHistorySegment, Market, OrderConstraints, OperationIntentConverter, OperationRequest}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Constant, Eval, IncrementalSeq, InputEval}

import java.time.{Duration, Instant}


object SingleMarketBot {

  case class State(
    time: Instant,
    baseBalance: BigDecimal,
    quoteBalance: BigDecimal,
    candleHistory: CandleHistorySegment,
  )

}

@deprecated("not complete yet")
abstract class SingleMarketBot extends EvalBot {

  def market: Market

  def orderConstraints: OrderConstraints

  def startTime: Instant

  def initialBaseBalance: BigDecimal

  def initialQuoteBalance: BigDecimal

  def candleLength: Duration

  def minimumCandleHistoryLength: Duration

  private def candleHistoryInput: CandleHistoryInput =
    CandleHistoryInput(
      market = market,
      start = startTime minus minimumCandleHistoryLength,
      candleLength = candleLength,
    )

  private val operationIntentConverter = OperationIntentConverter(market, Set())

  private val stateEval: Eval[SingleMarketBot.State] = Constant(
    SingleMarketBot.State(
      time = Instant.ofEpochSecond(0),
      baseBalance = BigDecimal(0),
      quoteBalance = BigDecimal(0),
      candleHistory = CandleHistorySegment.empty(Instant.ofEpochSecond(0), Duration.ofSeconds(0)),
    )
  )

  private val orderIntentEval = stateEval.map(s => getOrderIntents(s))

  private val newOperationRequestsEval: Eval[Seq[OperationRequest]] = {
//    val syncer = SimpleOrderIntentSyncer(OrderMatcher.ExactMatcher)

    for {
      intents <- orderIntentEval
//      openOrders <- inferredOpenOrdersEval
    } yield {
//      val operationIntents = syncer.apply(intents, openOrders.asInstanceOf[Set[Order.BasicOrderData]])
      operationIntentConverter.apply(intents)
    }
  }

  private val newOperationRequestMessages: Eval[Seq[OperationRequestMessage]] =
    NumberedOperationRequestMessagesEval(
      botIdEval = Constant(BotId("")),
      pastMessagesEval = InputEval(BotOutputHistory).collectIncremental { case orm: OperationRequestMessage => orm },
      newRequestsEval = newOperationRequestsEval,
    )

  override def eval: Eval[Iterable[BotOutput]] = newOperationRequestMessages

  protected def getOrderIntents(state: SingleMarketBot.State): Seq[OrderIntent]

}
