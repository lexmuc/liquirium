package io.liquirium.connect

import io.liquirium.core._

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

trait BasicExchangeConnector {

  def loadOpenOrders(tradingPair: TradingPair): Future[Set[Order]]

  def loadCandleHistory(
    tradingPair: TradingPair,
    candleLength: Duration
  )(
    start: Instant,
    maybeEnd: Option[Instant]
  ): Future[CandleHistorySegment]

  def loadTradeHistory(
    tradingPair: TradingPair
  )(
    start: Instant,
    maybeEnd: Option[Instant]
  ): Future[TradeHistorySegment]

  def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]]

}

object BasicExchangeConnector {

  def fromExchangeApi(api: GenericExchangeApi)(implicit ec: ExecutionContext): BasicExchangeConnector =
    new BasicExchangeConnector {

      override def loadOpenOrders(tradingPair: TradingPair): Future[Set[Order]] = ???

      override def loadCandleHistory(
        tradingPair: TradingPair,
        candleLength: Duration,
      )(
        start: Instant,
        maybeEnd: Option[Instant],
      ): Future[CandleHistorySegment] = {
        val segment = CandleHistorySegment.empty(start, candleLength)
        completeCandleHistorySegment(tradingPair, candleLength)(segment, maybeEnd)
      }

      private def completeCandleHistorySegment(
        tradingPair: TradingPair,
        candleLength: Duration,
      )(
        chs: CandleHistorySegment,
        maybeEnd: Option[Instant],
      ): Future[CandleHistorySegment] = {
        api.getCandleBatch(tradingPair, candleLength, chs.end).flatMap { b =>
          if (b.start != chs.end)
            throw new RuntimeException(s"Unexpected candle batch start: ${b.start}. Expected ${chs.end}")
          val extendedSegment = chs.extendWith(b.toHistorySegment)
          val finished = (b.nextBatchStart, maybeEnd) match {
            case (None, _) => true
            case (Some(nbs), Some(end)) => !end.isAfter(nbs)
            case _ => false
          }
          if (finished) {
            Future {
              maybeEnd match {
                case Some(end) => extendedSegment.truncate(end).padUntil(end)
                case None => extendedSegment
              }
            }
          }
          else {
            completeCandleHistorySegment(tradingPair, candleLength)(extendedSegment, maybeEnd)
          }
        }
      }

      override def loadTradeHistory(
        tradingPair: TradingPair,
      )(
        start: Instant,
        maybeEnd: Option[Instant],
      ): Future[TradeHistorySegment] = ???

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        api.sendTradeRequest(request)

    }

}
