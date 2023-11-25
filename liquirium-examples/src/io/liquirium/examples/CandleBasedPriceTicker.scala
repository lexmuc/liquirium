package io.liquirium.examples

import akka.actor.typed.ActorSystem
import io.liquirium.core.{CandleHistorySegment, TradingPair}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object CandleBasedPriceTicker extends App {

  // For the connector future
  private implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  // For markets with low trading volume candles may be empty. We load a few more so we can be reasonably sure to
  // get one with a price to start with.
  private val startSeconds = (Instant.now().getEpochSecond / 60) * 60 - 600 // round to whole minutes, subtract 10 minutes

  // A CandleHistorySegment is the sequence of all candles of the given length for a particular market after the
  // given start.  Initially, we pass an empty segment and Liquirium will inform us when the history is updated or
  // extended
  private val initialHistorySegment = CandleHistorySegment.empty(
    start = Instant.ofEpochSecond(startSeconds),
    candleLength = Duration.ofMinutes(1), // mind the API rate limit when using shorter candles!
  )

  for {
    connector <- io.liquirium.connect.binance.getConnector() // connector is obtained asynchronously
  } {
    // We use the default liquirium actor system, so we have no direct Akka dependency
    implicit val actorSystem: ActorSystem[Nothing] =
      io.liquirium.util.akka.DefaultConcurrencyContext.actorSystem

    // run an Akka Source that provides regular updates for the specified candle history segment.
    // Internally, Liquirium will poll the exchange API with a frequency higher than the frequency implied by
    // the candle length so we should get new candles one by one. Incomplete candles, i.e. candles that have not
    // ended yet, are returned -- and later updated -- if the exchange API returns incomplete candles.
    connector.candleHistoryStream(
      tradingPair = TradingPair("ETH", "BTC"),
      initialSegment = initialHistorySegment,
    ).runForeach { candleHistorySegment =>
      candleHistorySegment.lastOption match {
        case None => () // No candles after the given start so far. Should not happen when start is in the past.
        case Some(candle) =>
          println(s"Latest price is ${candle.close.toString()}")
      }
    }
  }

}
