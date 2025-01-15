package io.liquirium.examples.ticker

import io.liquirium.core.{CandleHistorySegment, TradingPair}
import io.liquirium.util.akka.DefaultConcurrencyContext

import java.time.{Duration, Instant}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object CandleBasedPriceTicker extends App {

  import DefaultConcurrencyContext.executionContext
  private val connector = Await.result(io.liquirium.connect.binance.getConnectorWithSubscriptions(), 10.seconds)

  private val tradingPair = TradingPair("ETH", "BTC")

  println("Running price ticker for " + tradingPair.toString)

  // A CandleHistorySegment is the sequence of all candles of the given length for a particular market after the
  // given start.  Initially, we pass an empty segment and Liquirium will inform us when the history is updated or
  // extended
  private val initialHistorySegment = CandleHistorySegment.empty(
    start = roundDownToFullMinute(Instant.now()).minusSeconds(60),
    candleLength = Duration.ofMinutes(1), // mind the API rate limit when using shorter candles!
  )

  connector.candleHistorySubscription(tradingPair, initialHistorySegment).run(candleHistorySegment =>
    candleHistorySegment.lastOption match {
      case None =>
        println(s"${candleHistorySegment.end}: No price data available yet")
      case Some(candle) =>
        println(s"${candleHistorySegment.end}: Latest price is ${candle.close.toString()}")
    }
  )

  // Run an Akka Source that provides regular updates for the specified candle history segment.
  // Internally, Liquirium will poll the exchange API with a frequency higher than the frequency implied by
  // the candle length so we should get new candles one by one. Incomplete candles, i.e. candles that have not
  // ended yet, are returned -- and later updated -- if the exchange API returns incomplete candles.
  //  connector.candleHistoryStream(
  //    tradingPair = tradingPair,
  //    initialSegment = initialHistorySegment,
  //  ).runForeach { candleHistorySegment =>
  //    candleHistorySegment.lastOption match {
  //      case None =>
  //        println(s"${candleHistorySegment.end}: No price data available yet")
  //      case Some(candle) =>
  //        println(s"${candleHistorySegment.end}: Latest price is ${candle.close.toString()}")
  //    }
  //  }

  private def roundDownToFullMinute(instant: Instant): Instant = {
    val seconds = instant.getEpochSecond
    Instant.ofEpochSecond(seconds - seconds % 60)
  }

}
