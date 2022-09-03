package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput._
import io.liquirium.bot.OperationRequestMessage
import io.liquirium.core.TradeRequestResponseMessage.NoSuchOpenOrderCancelFailure
import io.liquirium.core._
import io.liquirium.core.orderTracking.{OpenOrdersHistory, OpenOrdersSnapshot}
import io.liquirium.eval._
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant

trait CandleSimulatorMarketplace extends SimulationMarketplace {

  override def processOperationRequest(
    requestMessage: OperationRequestMessage,
    context: UpdatableContext,
  ): Either[InputRequest, (InputUpdate, CandleSimulatorMarketplace)]

  override def processPriceUpdates(
    newContext: UpdatableContext,
  ): Either[InputRequest, (UpdatableContext, CandleSimulatorMarketplace)]

}

object CandleSimulatorMarketplace {

  def apply(
    market: Market,
    candlesEval: Eval[CandleHistorySegment],
    simulator: CandleSimulator,
    orderIds: Stream[String],
    lastCandleEndTime: Instant,
  ): CandleSimulatorMarketplace = Impl(
    market = market,
    candlesEval = candlesEval,
    simulator = simulator,
    orderIds = orderIds,
    lastCandleEndTime = lastCandleEndTime,
  )

  case class Impl(
    market: Market,
    candlesEval: Eval[CandleHistorySegment],
    simulator: CandleSimulator,
    orderIds: Stream[String],
    lastCandleEndTime: Instant,
  ) extends CandleSimulatorMarketplace {

    private val orderHistoryInput: OrderSnapshotHistoryInput = OrderSnapshotHistoryInput(market)
    private val tradeHistoryInput = TradeHistoryInput(market, Instant.ofEpochSecond(0))

    override def processOperationRequest(
      requestMessage: OperationRequestMessage,
      context: UpdatableContext,
    ): Either[InputRequest, (InputUpdate, Impl)] = {
      if (requestMessage.request.market != market)
        throw new RuntimeException(
          s"simulation marketplace for $market got request for ${requestMessage.request.market}"
        )

      val tupleEval = for {
        candleHistory <- candlesEval
        orderHistory <- InputEval(orderHistoryInput)
        completedRequests <- InputEval(CompletedOperationRequestsInSession)
      } yield (candleHistory, orderHistory, completedRequests)

      context.evaluate(tupleEval)._1 match {
        case Value((candleHistory, orderHistory, completedOperationRequests)) =>
          requestMessage.request match {
            case or: OrderRequest =>
              val result = processOrderRequest(
                or = or,
                time = candleHistory.end,
                requestMessage = requestMessage,
                oldOrderHistory = orderHistory,
                completedOperationRequests = completedOperationRequests,
              )
              Right(result)

            case cr: CancelRequest =>
              val result = processCancelRequest(
                cr = cr,
                time = candleHistory.end,
                requestMessage = requestMessage,
                oldOrderHistory = orderHistory,
                completedOperationRequests = completedOperationRequests,
              )
              Right(result)
          }

        case ir: InputRequest => Left(ir)
      }
    }

    private def processOrderRequest(
      or: OrderRequest,
      time: Instant,
      requestMessage: OperationRequestMessage,
      oldOrderHistory: OpenOrdersHistory,
      completedOperationRequests: IncrementalSeq[CompletedOperationRequest],
    ): (InputUpdate, Impl) =  {
      val newOrder = or.toExactOrder(orderIds.head)
      val newState = copy(
        orderIds = orderIds.tail,
      )
      val newOrderSet = oldOrderHistory.lastSnapshot.orders + newOrder
      val newOrderHistory = oldOrderHistory.appendIfChanged(
        OpenOrdersSnapshot(newOrderSet, time),
      )
      val confirmation = OrderRequestConfirmation(Some(newOrder), Seq())
      val newCompletedRequest = CompletedOperationRequest(time, requestMessage, Right(confirmation))
      val inputUpdate: InputUpdate = InputUpdate(Map(
        orderHistoryInput -> newOrderHistory,
        CompletedOperationRequestsInSession -> completedOperationRequests.inc(newCompletedRequest),
      ))
      (inputUpdate, newState)
    }

    private def processCancelRequest(
      cr: CancelRequest,
      time: Instant,
      requestMessage: OperationRequestMessage,
      oldOrderHistory: OpenOrdersHistory,
      completedOperationRequests: IncrementalSeq[CompletedOperationRequest]
    ): (InputUpdate, Impl) = {
      oldOrderHistory.lastSnapshot.orders.find(_.id == cr.orderId) match {
        case Some(order) =>
          val newOrderSet = oldOrderHistory.lastSnapshot.orders.removeId(cr.orderId)
          val newOrderHistory = oldOrderHistory.appendIfChanged(
            OpenOrdersSnapshot(newOrderSet, time),
          )
          val confirmation = CancelRequestConfirmation(Some(AbsoluteQuantity(order.openQuantity.abs)))
          val newCompletedRequest = CompletedOperationRequest(time, requestMessage, Right(confirmation))
          val inputUpdate = InputUpdate(Map(
            orderHistoryInput -> newOrderHistory,
            CompletedOperationRequestsInSession -> completedOperationRequests.inc(newCompletedRequest),
          ))
          (inputUpdate, copy())

        case None =>
          val failure = NoSuchOpenOrderCancelFailure(cr.orderId)
          val newCompletedRequest = CompletedOperationRequest(time, requestMessage, Left(failure))
          val inputUpdate = InputUpdate(Map(
            CompletedOperationRequestsInSession -> completedOperationRequests.inc(newCompletedRequest),
          ))
          (inputUpdate, copy())
      }

    }

    override def processPriceUpdates(
      context: UpdatableContext,
    ): Either[InputRequest, (UpdatableContext, Impl)] = {
      val threeTupleEval = for {
        cc <- candlesEval
        th <- InputEval(tradeHistoryInput)
        oh <- InputEval(orderHistoryInput)
      } yield (cc, th, oh)
      context(threeTupleEval) match {
        case Value((chs: CandleHistorySegment, tradeHistory: TradeHistorySegment, orderHistory: OpenOrdersHistory)) =>
          val ((finalOrderHistory, allNewTrades), newState) =
            newCandles(chs).foldLeft((orderHistory, Seq[Trade]()), this) {
              case (((oo, tt), self), c) =>
                val (newOo, newTrades, newState) = self.processCandle(c, oo)
                ((newOo, tt ++ newTrades), newState)
            }

          val inputUpdate =
            if (finalOrderHistory == orderHistory && allNewTrades.isEmpty) {
              InputUpdate(Map())
            }
            else {
              InputUpdate(
                getTradeHistoryUpdates(tradeHistory, allNewTrades)
                  .updated(orderHistoryInput, finalOrderHistory)
              )
            }
          Right((context.update(inputUpdate), newState))
        case ir: InputRequest => Left(ir)
      }
    }

    private def getTradeHistoryUpdates(
      tradeHistory: TradeHistorySegment,
      newTrades: Seq[Trade],
    ): Map[Input[_], TradeHistorySegment] = {
      val newTradeHistory = newTrades.foldLeft(tradeHistory) { case (historySegment, t) => historySegment.inc(t) }
      Map(tradeHistoryInput -> newTradeHistory)
    }

    private def newCandles(candles: CandleHistorySegment): Iterable[Candle] =
      candles.reverseIterator.takeWhile(_.endTime isAfter lastCandleEndTime).toList.reverse

    private def processCandle(
      c: Candle,
      orderHistory: OpenOrdersHistory,
    ): (OpenOrdersHistory, Seq[Trade], Impl) = {
      val openOrders = orderHistory.lastSnapshot.orders
      val (newTrades, newOrders, newSim) = simulator.fillOrders(openOrders, c)
      val newOrderHistory = orderHistory.appendIfChanged(OpenOrdersSnapshot(newOrders, c.endTime))
      val newState = copy(
        simulator = newSim,
        lastCandleEndTime = c.endTime,
      )
      (newOrderHistory, newTrades, newState)
    }

  }

}

