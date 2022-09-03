package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest._
import io.liquirium.connect.{CandleBatch, GenericExchangeApi, TradeBatch}
import io.liquirium.core._
import io.liquirium.util.akka.AsyncApi

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class PoloniexApiAdapter(
  restApi: AsyncApi[PoloniexApiRequest[_]],
  modelConverter: PoloniexModelConverter,
  maxCandleBatchSize: Int = 500, //should be the maximum allowed by the api
  maxTradeBatchSize: Int = 1000, //should be the maximum allowed by the api
  maxOrderBatchSize: Int = 2000, //should be the maximum allowed by the api
)(
  implicit executionContext: ExecutionContext,
) extends GenericExchangeApi {

  override def getCandleBatch(tradingPair: TradingPair, candleLength: Duration, start: Instant): Future[CandleBatch] = {
    val request = GetCandles(
      symbol = modelConverter.getSymbol(tradingPair),
      interval = PoloniexCandleLength.forDuration(candleLength),
      limit = Some(maxCandleBatchSize),
      startTime = Some(start),
      endTime = None,
    )
    restApi.sendRequest(request).map { poloniexCandleBatch =>
      val convertedCandles = poloniexCandleBatch.map(x => modelConverter.convertCandle(x))
      CandleBatch(
        start = start,
        candleLength = candleLength,
        candles = convertedCandles,
        nextBatchStart = getNextBatchStart(convertedCandles),
      )
    }
  }

  private def getNextBatchStart(batchCandles: Iterable[Candle]): Option[Instant] =
    if (batchCandles.size == maxCandleBatchSize) Some(batchCandles.last.endTime) else None

  override def getTradeBatch(tradingPair: TradingPair, start: Instant): Future[TradeBatch] = {
    val request = GetTradeHistory(
      limit = Some(maxTradeBatchSize),
      endTime = None,
      startTime = Some(start),
      from = None,
      direction = None,
      symbols = List(modelConverter.getSymbol(tradingPair))
    )
    restApi.sendRequest(request).map { poloniexTrades =>

      val convertedAndSortedTrades = poloniexTrades.map(modelConverter.convertTrade).sorted(Ordering[HistoryEntry])

      TradeBatch(
        start = start,
        trades = convertedAndSortedTrades,
        nextBatchStart = getNextTradeBatchStart(convertedAndSortedTrades),
      )
    }
  }

  private def getNextTradeBatchStart(trades: Seq[Trade]): Option[Instant] =
    if (trades.size == maxTradeBatchSize) {
      if (trades.head.time == trades.last.time)
        throw new RuntimeException("All trades in batch have the same timestamp. " +
          "Cannot advance start time for next batch. Can be fixed with larger batches (limit parameter)")
      Some(trades.last.time)
    } else None

  override def getOpenOrders(tradingPair: TradingPair): Future[Set[Order]] = {
    val request = GetOpenOrders(
      symbol = Option(modelConverter.getSymbol(tradingPair)),
      side = None,
      from = None,
      direction = None,
      limit = Option(maxOrderBatchSize),
    )
    restApi.sendRequest(request).map(coinbaseOrders =>
      coinbaseOrders.map(modelConverter.convertOrder).toSet
    )
  }

  override def sendTradeRequest[T <: OperationRequest](tradeRequest: T): Future[OperationRequestSuccessResponse[T]] =
    tradeRequest match {
      case or: OrderRequest =>
        val pot = PoloniexOrderType
        val poloniexRequest = CreateOrder(
          symbol = modelConverter.getSymbol(or.market.tradingPair),
          side = if (or.quantity.signum > 0) Side.Buy else Side.Sell,
          timeInForce = None,
          `type` = if (or.modifiers(OrderModifier.PostOnly)) Option(pot.LIMIT_MAKER) else Option(pot.LIMIT),
          accountType = None,
          price = Option(or.price),
          quantity = Option(or.quantity.abs),
          amount = None,
          clientOrderId = None,
        )
        restApi.sendRequest(poloniexRequest).map(pcor => {
          val order = Order(
            id = pcor.id,
            market = or.market,
            openQuantity = or.quantity,
            fullQuantity = or.quantity,
            price = or.price
          )
          OrderRequestConfirmation(Some(order), Seq())
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        }
        )

      case cr: CancelRequest =>
        val poloniexRequest = CancelOrderById(
          orderId = cr.orderId
        )
        restApi.sendRequest(poloniexRequest).map { pcobir =>
          // PoloniexCancelOrderByIdResponse has no information on quantity
          CancelRequestConfirmation(None)
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        }
    }

  override def getOrderConstraintsByMarket(): Future[Map[Market, OrderConstraints]] = {
    val request = GetSymbolInfos()
    restApi.sendRequest(request).map { poloniexSymbolInfos =>
      poloniexSymbolInfos.map(psi => modelConverter.getMarket(psi.symbol) -> modelConverter.convertSymbolInfo(psi)).toMap
    }
  }

}
