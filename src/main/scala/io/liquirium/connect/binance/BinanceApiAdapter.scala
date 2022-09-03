package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.connect.{CandleBatch, GenericExchangeApi, TradeBatch}
import io.liquirium.core._
import io.liquirium.util.AbsoluteQuantity
import io.liquirium.util.akka.AsyncApi

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class BinanceApiAdapter(
  restApi: AsyncApi[BinanceApiRequest[_]],
  modelConverter: BinanceModelConverter,
  maxCandleBatchSize: Int = 1000, // default should be the maximum allowed by the api
  maxTradeBatchSize: Int = 1000, // should be the maximum allowed by the api
)(
  implicit executionContext: ExecutionContext,
) extends GenericExchangeApi {

  override def getCandleBatch(tradingPair: TradingPair, candleLength: Duration, start: Instant): Future[CandleBatch] = {
    val request = BinanceRestApi.CandlesRequest(
      symbol = modelConverter.getSymbol(tradingPair),
      interval = BinanceCandleLength.forDuration(candleLength),
      limit = Some(maxCandleBatchSize),
      from = Some(start),
      until = None,
    )
    restApi.sendRequest(request).map { binanceCandles =>
      val convertedCandles = binanceCandles.map(modelConverter.convertCandle)
      CandleBatch(
        start = start,
        candleLength = candleLength,
        candles = convertedCandles.filter(_.length == candleLength),
        nextBatchStart = getNextCandleBatchStart(convertedCandles),
      )
    }
  }

  private def getNextCandleBatchStart(batchCandles: Iterable[Candle]): Option[Instant] =
    if (batchCandles.size == maxCandleBatchSize) Some(batchCandles.last.endTime) else None

  override def getTradeBatch(tradingPair: TradingPair, start: Instant): Future[TradeBatch] = {
    val request = BinanceRestApi.GetTradesRequest(
      symbol = modelConverter.getSymbol(tradingPair),
      startTime = Some(start),
      endTime = None,
      limit = Some(maxTradeBatchSize),
    )
    restApi.sendRequest(request).map { binanceTrades =>
      val convertedAndSortedTrades = binanceTrades.map(modelConverter.convertTrade).sorted(Ordering[HistoryEntry])

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
    val request = BinanceRestApi.OpenOrdersRequest(
      symbol = Some(modelConverter.getSymbol(tradingPair)),
    )
    restApi.sendRequest(request).map { oo =>
      oo.map(modelConverter.convertOrder).toSet
    }
  }

  override def sendTradeRequest[T <: OperationRequest](tradeRequest: T): Future[OperationRequestSuccessResponse[T]] =
    tradeRequest match {

      case or: OrderRequest =>
        val bot = BinanceOrderType
        val binanceRequest = BinanceRestApi.NewOrderRequest(
          side = if (or.quantity.signum > 0) Side.Buy else Side.Sell,
          symbol = modelConverter.getSymbol(or.market.tradingPair),
          quantity = or.quantity.abs,
          price = or.price,
          orderType = if (or.modifiers(OrderModifier.PostOnly)) bot.LIMIT_MAKER else bot.LIMIT,
        )
        restApi.sendRequest(binanceRequest).map(bo =>
          OrderRequestConfirmation(Some(modelConverter.convertOrder(bo)), Seq())
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        )

      case cr: CancelRequest =>
        val binanceRequest = BinanceRestApi.CancelOrderRequest(
          symbol = modelConverter.getSymbol(cr.market.tradingPair),
          orderId = cr.orderId,
        )
        restApi.sendRequest(binanceRequest).map { bo =>
          val rest = bo.originalQuantity - bo.executedQuantity
          CancelRequestConfirmation(Some(AbsoluteQuantity(rest)))
            .asInstanceOf[OperationRequestSuccessResponse[T]]
        }

    }

  override def getOrderConstraintsByMarket(): Future[Map[Market, OrderConstraints]] = {
    val request = BinanceRestApi.GetExchangeInfo()
    restApi.sendRequest(request).map { binanceSymbolInfos =>
      binanceSymbolInfos.map(si => (modelConverter.getMarket(si.symbol), modelConverter.convertSymbolInfo(si))).toMap
    }
  }

}
