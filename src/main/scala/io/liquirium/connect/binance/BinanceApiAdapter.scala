package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.connect.{ForwardCandleBatch, GenericExchangeApi}
import io.liquirium.core.{Candle, TradingPair}
import io.liquirium.util.akka.AsyncApi

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class BinanceApiAdapter(
  restApi: AsyncApi[BinanceApiRequest[_]],
  modelConverter: BinanceModelConverter,
  maxCandleBatchSize: Int = 1000, // should be the maximum allowed by the api
)(
  implicit executionContext: ExecutionContext,
) extends GenericExchangeApi {

  override def getForwardCandleBatch(
    tradingPair: TradingPair,
    resolution: Duration,
    start: Instant,
  ): Future[ForwardCandleBatch] =
    try {
      val request = BinanceRestApi.CandlesRequest(
        symbol = modelConverter.getSymbol(tradingPair),
        resolution = BinanceCandleResolution.forDuration(resolution),
        limit = Some(maxCandleBatchSize),
        from = Some(start),
        until = None,
      )
      restApi.sendRequest(request).map { binanceCandleBatch =>
        val convertedCandles = binanceCandleBatch.entries.map(modelConverter.convertCandle)
        ForwardCandleBatch(
          start = start,
          resolution = resolution,
          candles = convertedCandles,
          nextBatchStart = getNextBatchStart(convertedCandles),
        )
      }
    } catch {
      case t: Throwable => Future.failed(t)
    }

  private def getNextBatchStart(batchCandles: Iterable[Candle]): Option[Instant] =
    if (batchCandles.size == maxCandleBatchSize) Some(batchCandles.last.endTime) else None

}
