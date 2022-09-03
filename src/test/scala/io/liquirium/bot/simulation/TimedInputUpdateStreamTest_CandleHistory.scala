package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput
import io.liquirium.bot.BotInput.CandleHistoryInput
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.c10
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.core.{Candle, CandleHistorySegment}


class TimedInputUpdateStreamTest_CandleHistory extends BasicTest {

  private def stream(chi: BotInput.CandleHistoryInput, candles: Iterable[Candle]) =
    TimedInputUpdateStream.forCandleHistory(chi, candles)

  test("it starts at the beginning of the segment and contains updates for each candle at the respective time") {
    val candles = List(
      c10(sec(20), 2),
      c10(sec(30), 3),
      c10(sec(40), 4),
    )
    val input = CandleHistoryInput(market(1), secs(10), sec(20))
    val s0 = CandleHistorySegment.empty(sec(20), secs(10))
    val s1 = s0.append(c10(sec(20), 2))
    val s2 = s1.append(c10(sec(30), 3))
    val s3 = s2.append(c10(sec(40), 4))
    stream(input, candles).toList shouldEqual List(
      sec(20) -> s0,
      sec(30) -> s1,
      sec(40) -> s2,
      sec(50) -> s3,
    )
  }

  test("the segments in the stream are in fact incremental") {
    val input = CandleHistoryInput(market(1), secs(10), sec(20))
    val candles = List(c10(sec(20), 2), c10(sec(30), 3))
    val inputUpdates = stream(input, candles).map(_._2).toList
    inputUpdates(1).prev.get should be theSameInstanceAs inputUpdates(0)
    inputUpdates(2).prev.get should be theSameInstanceAs inputUpdates(1)
  }

}
