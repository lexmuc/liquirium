package io.liquirium.examples.connect

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import io.liquirium.core.{CandleHistorySegment, TradingPair}
import io.liquirium.util.ApiCredentials
import io.liquirium.util.akka.DefaultConcurrencyContext

import java.time.{Duration, Instant}
import scala.concurrent.ExecutionContext

object CandleBasedPriceTicker extends App {

  // Usually we can just use the ExecutionContext and ActorSystem of the DefaultConcurrencyContext provided
  // by Liquirium.
  private implicit val executionContext: ExecutionContext = DefaultConcurrencyContext.executionContext
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = DefaultConcurrencyContext.actorSystem

  // For candles we don't need credentials
  val apiCredentials = ApiCredentials("", "")

  val tradingPair = TradingPair("ETH", "BTC")
  // A CandleHistorySegment is the sequence of all candles of the given length for a particular market after the
  // given start.  Initially, we pass an empty segment and Liquirium will inform us when updated or more candles
  // are available
  val initialHistorySegment = CandleHistorySegment.empty(
    start = Instant.parse("2023-05-27T12:00:00.000Z"),
    candleLength = Duration.ofMinutes(1), // mind the API rate limit when using shorter candles!
  )

  for {
    // connector is obtained asynchronously
    connector <- io.liquirium.connect.binance.connector(DefaultConcurrencyContext, apiCredentials)
  } {
    // run an Akka Source that provides regular updates for the specified candle history segment.
    // Internally, Liquirium will poll the exchange API with a frequency higher than the frequency implied by
    // the candle length so we should get new candles one by one as they appear. Incomplete candles, i.e. candles that
    // have not ended yet, are returned -- and eventually updated -- when the exchange API returns incomplete candles.
    connector.candleHistoryStream(tradingPair, initialHistorySegment).runForeach {
      candleHistorySegment => candleHistorySegment.lastOption match {
        case None => () // No candles after the given start so far. Should not happen when start is in the past.
        case Some(candle) =>
          println(s"Latest price is ${candle.close.toString()}")
      }
    }
  }

}
