package io.liquirium.core

import io.liquirium.util.store.{TradeSegmentStartStore, TradeStore}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class TradeHistoryCache(
  tradeStore: TradeStore,
  startStore: TradeSegmentStartStore,
)(
  implicit ec: ExecutionContext,
) {

  def read(): Future[Option[TradeHistorySegment]] =
    startStore.readStart.flatMap {
      case Some(start) =>
        tradeStore.get(Some(start), None).map { trades =>
          Some(TradeHistorySegment.fromForwardTrades(start, trades.trades))
        }
      case None => Future.successful(None)
    }

  def save(segment: TradeHistorySegment): Future[Unit] = {
    for {
      _ <- startStore.saveStart(segment.start)
      _ <- tradeStore.deleteFrom(Instant.ofEpochMilli(0))
      _ <- if (segment.isEmpty) Future.successful(()) else tradeStore.add(segment)
    } yield ()
  }

  def extendWith(extension: TradeHistorySegment): Future[Unit] =
    startStore.readStart flatMap {
      case None => throw new RuntimeException("Cannot extend an empty trade history")
      case Some(start) if extension.start == start => replaceFrom(start, extension)
      case Some(start) if extension.start isAfter start =>
        tradeStore.get(from = Some(extension.start), until = None) flatMap { tt =>
          println(tt)
          if (tt.trades.isEmpty)
            throw new RuntimeException("Cannot extend with a segment that does not overlap at the end")
          else if (tt.trades.head.time != extension.start)
            throw new RuntimeException("Cannot extend with a segment that does not overlap at the end")
          replaceFrom(extension.start, extension)
        }
      case Some(_) =>
        throw new RuntimeException("Extension starts before the cached segment")
    }

  private def replaceFrom(time: Instant, newTrades: Iterable[Trade]): Future[Unit] = for {
    _ <- tradeStore.deleteFrom(time)
    _ <- tradeStore.add(newTrades)
  } yield ()

}
