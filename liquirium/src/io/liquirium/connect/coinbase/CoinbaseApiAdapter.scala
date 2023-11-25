package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest._
import io.liquirium.connect.{CandleBatch, GenericExchangeApi, TradeBatch}
import io.liquirium.core.OrderModifier.PostOnly
import io.liquirium.core._
import io.liquirium.util.Clock
import io.liquirium.util.akka.AsyncApi

import java.time.{Duration, Instant}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CoinbaseApiAdapter(
  restApi: AsyncApi[CoinbaseApiRequest[_]],
  modelConverter: CoinbaseModelConverter,
  maxCandleBatchSize: Int = 300, // should be the maximum allowed by the api
  maxTradeBatchSize: Int = 3000, // value wasn't stated by Api response but error when setting limit to 3001
  maxOrderBatchSize: Int = 3000,
  clock: Clock,
)(
  implicit executionContext: ExecutionContext,
) extends GenericExchangeApi {

  override def getCandleBatch(tradingPair: TradingPair, candleLength: Duration, start: Instant): Future[CandleBatch] =
    try {
      val nextBatchStart = Instant.ofEpochSecond(start.getEpochSecond + maxCandleBatchSize * candleLength.getSeconds)
      val nextBatchStartBeforeNow = nextBatchStart.isBefore(clock.getTime)
      val request = GetCandles(
        productId = modelConverter.getProductId(tradingPair),
        start = start,
        end = nextBatchStart.minusSeconds(1),
        granularity = CoinbaseCandleLength.forDuration(candleLength),
      )
      restApi.sendRequest(request).map { coinbaseCandleBatch =>
        val convertedCandles = coinbaseCandleBatch.map(x => modelConverter.convertCandle(x, candleLength))
        CandleBatch(
          start = start,
          candleLength = candleLength,
          candles = convertedCandles,
          nextBatchStart = if (nextBatchStartBeforeNow) Some(nextBatchStart) else None,
        )
      }
    } catch {
      case t: Throwable => Future.failed(t)
    }


  override def getTradeBatch(tradingPair: TradingPair, start: Instant): Future[TradeBatch] = {
    try {
      val request = GetTradeHistory(
        orderId = None,
        productId = Option(modelConverter.getProductId(tradingPair)),
        startSequenceTimestamp = Some(start),
        endSequenceTimestamp = None,
        limit = Some(maxTradeBatchSize),
      )
      restApi.sendRequest(request).map { coinbaseTrades =>
        val convertedAndSortedTrades = coinbaseTrades.map(modelConverter.convertTrade).sorted(Ordering[HistoryEntry])

        TradeBatch(
          start = start,
          trades = convertedAndSortedTrades,
          nextBatchStart = getNextTradeBatchStart(convertedAndSortedTrades),
        )
      }
    } catch {
      case t: Throwable => Future.failed(t)
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
    try {
      val request = GetOpenOrders(
        productId = Option(modelConverter.getProductId(tradingPair)),
        limit = maxOrderBatchSize,
      )
      restApi.sendRequest(request).map(coinbaseOrders =>
        coinbaseOrders.map(modelConverter.convertOrder).toSet
      )
    } catch {
      case t: Throwable => Future.failed(t)
    }
  }

  override def sendTradeRequest[T <: OperationRequest](tradeRequest: T): Future[OperationRequestSuccessResponse[T]] = {
    tradeRequest match {
      case or: OrderRequest =>
        try {
          val request = CreateOrder(
            side = if (or.isBuy) Side.Buy else Side.Sell,
            productId = modelConverter.getProductId(or.market.tradingPair),
            clientOrderId = UUID.randomUUID().toString, //Client set unique uuid for this order
            baseSize = or.quantity,
            limitPrice = or.price,
            postOnly = or.modifiers.contains(PostOnly),
          )
          restApi.sendRequest(request).map {
            case s: CoinbaseCreateOrderResponse.Success =>
              val order = Order(
                id = s.orderId,
                market = or.market,
                openQuantity = or.quantity,
                fullQuantity = or.quantity,
                price = or.price
              )
              OrderRequestConfirmation(Some(order), Seq()).asInstanceOf[OperationRequestSuccessResponse[T]]
            case f: CoinbaseCreateOrderResponse.Failure =>
              throw new RuntimeException(f.error)
          }

        } catch {
          case t: Throwable => Future.failed(t)
        }

      case cr: CancelRequest =>
        val coinbaseRequest = CancelOrders(
          orderIds = Seq(cr.orderId)
        )
        restApi.sendRequest(coinbaseRequest).map { ccortls =>
          // cancelOrderS -> Seq(res)
          val coinbaseCancelOrderResult = ccortls.head
          if (coinbaseCancelOrderResult.success) {
            CancelRequestConfirmation(None).asInstanceOf[OperationRequestSuccessResponse[T]]
          } else {
            throw new RuntimeException(coinbaseCancelOrderResult.failureReason)
          }
        }
    }
  }

  override def getOrderConstraintsByMarket(): Future[Map[Market, OrderConstraints]] = {
    val request = CoinbaseApiRequest.ListProducts()
    restApi.sendRequest(request).map { coinbaseProductInfos =>
      coinbaseProductInfos.map(cpi => (modelConverter.getMarket(cpi.symbol), modelConverter.convertProductInfo(cpi))).toMap
    }
  }

}
