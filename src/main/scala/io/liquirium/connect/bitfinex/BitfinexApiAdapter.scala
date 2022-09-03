package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.BitfinexApiRequest
import io.liquirium.connect.{CandleBatch, GenericExchangeApi, TradeBatch}
import io.liquirium.core._
import io.liquirium.util.akka.AsyncApi
import io.liquirium.util.{AbsoluteQuantity, ResultOrder}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class BitfinexApiAdapter(
  restApi: AsyncApi[BitfinexApiRequest[_]],
  modelConverter: BitfinexModelConverter,
  maxCandleBatchSize: Int = 5000, // should be the maximum allowed by the api
  maxTradeBatchSize: Int = 2500,
  maxOrderHistoryBatchSize: Int = 2500,
)(
  implicit executionContext: ExecutionContext,
) extends GenericExchangeApi {

  override def getCandleBatch(tradingPair: TradingPair, candleLength: Duration, start: Instant): Future[CandleBatch] =
    try {
      val request = BitfinexRestApi.GetCandles(
        symbol = modelConverter.getSymbol(tradingPair),
        candleLength = BitfinexCandleLength.forDuration(candleLength),
        limit = Some(maxCandleBatchSize),
        from = Some(start),
        until = None,
        sort = ResultOrder.AscendingOrder,
      )
      restApi.sendRequest(request).map { bitfinexCandleBatch =>
        val convertedCandles = bitfinexCandleBatch.map(x => modelConverter.convertCandle(x, candleLength))
        CandleBatch(
          start = start,
          candleLength = candleLength,
          candles = convertedCandles,
          nextBatchStart = getNextBatchStart(convertedCandles),
        )
      }
    } catch {
      case t: Throwable => Future.failed(t)
    }

  private def getNextBatchStart(batchCandles: Iterable[Candle]): Option[Instant] =
    if (batchCandles.size == maxCandleBatchSize) Some(batchCandles.last.endTime) else None


  override def getTradeBatch(tradingPair: TradingPair, start: Instant): Future[TradeBatch] = {
    val bitfinexRequest = BitfinexRestApi.GetTradeHistory(
      symbol = Some(modelConverter.getSymbol(tradingPair)),
      from = Some(start),
      until = None,
      limit = maxTradeBatchSize,
      sort = ResultOrder.AscendingOrder,
    )
    restApi.sendRequest(bitfinexRequest).map { bitfinexTrades =>
      val convertedAndSortedTrades = bitfinexTrades.map(modelConverter.convertTrade).sorted(Ordering[HistoryEntry])

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
    val request = BitfinexRestApi.GetOpenOrders(
      symbol = Some(modelConverter.getSymbol(tradingPair))
    )
    restApi.sendRequest(request).map(bitfinexOrders =>
      bitfinexOrders.map(modelConverter.convertOrder).toSet
    )
  }


  override def sendTradeRequest[T <: OperationRequest](tradeRequest: T): Future[OperationRequestSuccessResponse[T]] =

    tradeRequest match {

      case or: OrderRequest =>
        val bot = BitfinexOrder.OrderType
        val request = BitfinexRestApi.SubmitOrder(
          `type` = bot.ExchangeLimit,
          symbol = modelConverter.getSymbol(or.market.tradingPair),
          price = Some(or.price),
          amount = or.quantity,
          flags = if (or.modifiers(OrderModifier.PostOnly)) Set(BitfinexOrderFlag.PostOnly) else Set(),
        )
        restApi.sendRequest(request).map(bo => {
          val order = Order(
            id = bo.id.toString,
            market = or.market,
            openQuantity = bo.amount,
            fullQuantity = bo.originalAmount,
            price = bo.price,
          )
          OrderRequestConfirmation(Some(order), Seq())
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        })

      case cr: CancelRequest =>
        val request = BitfinexRestApi.CancelOrder(
          id = cr.orderId.toInt
        )
        restApi.sendRequest(request).map { bo =>
          CancelRequestConfirmation(Some(AbsoluteQuantity(bo.amount.abs)))
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        }

    }

  override def getOrderConstraintsByMarket(): Future[Map[Market, OrderConstraints]] = {
    val request = BitfinexRestApi.GetPairInfos()
    restApi.sendRequest(request).map { bitfinexPairInfos =>
      bitfinexPairInfos.map(bpi => modelConverter.getMarketFromPair(bpi.pair) -> modelConverter.convertPairInfo(bpi)).toMap
    }
  }

}
