package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput
import io.liquirium.bot.BotInput.TradeHistoryInput
import io.liquirium.core.{Trade, TradeHistorySegment}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.core.helpers.TradeHelpers.trade
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TimedInputUpdateStreamTest_TradeHistory extends BasicTest {

  private def stream(thi: BotInput.TradeHistoryInput, trades: Iterable[Trade]) =
    TimedInputUpdateStream.forTradeHistory(thi, trades)

  test("it starts at the beginning of the segment and contains updates for each trade at the respective time") {
    val input = TradeHistoryInput(market(1), sec(20))
    val trades = List(
      trade(sec(30), "A"),
      trade(sec(40), "B"),
      trade(sec(50), "C"),
    )
    val s0 = TradeHistorySegment.empty(sec(20))
    val s1 = s0.append(trade(sec(30), "A"))
    val s2 = s1.append(trade(sec(40), "B"))
    val s3 = s2.append(trade(sec(50), "C"))
    stream(input, trades).toList shouldEqual List(
      sec(20) -> s0,
      sec(30) -> s1,
      sec(40) -> s2,
      sec(50) -> s3,
    )
  }

  test("trades at the same time are grouped into one update") {
    val input = TradeHistoryInput(market(1), sec(20))
    val trades = List(
      trade(sec(30), "A-1"),
      trade(sec(30), "A-2"),
    )
    val s0 = TradeHistorySegment.empty(sec(20))
    val s1 = s0.append(trade(sec(30), "A-1")).append(trade(sec(30), "A-2"))
    stream(input, trades).toList shouldEqual List(
      sec(20) -> s0,
      sec(30) -> s1,
    )
  }

  test("the first update already contains trades at the very start") {
    val input = TradeHistoryInput(market(1), sec(30))
    val trades = List(
      trade(sec(30), "A-1"),
      trade(sec(30), "A-2"),
    )
    val s0 = TradeHistorySegment.empty(sec(30))
    val s1 = s0.append(trade(sec(30), "A-1")).append(trade(sec(30), "A-2"))
    stream(input, trades).toList shouldEqual List(
      sec(30) -> s1,
    )
  }

  test("if the trades are empty there is only one update with the empty segment") {
    val input = TradeHistoryInput(market(1), sec(20))
    val trades = List()
    val s0 = TradeHistorySegment.empty(sec(20))
    stream(input, trades).toList shouldEqual List(
      sec(20) -> s0,
    )
  }

  test("the segments in the stream are in fact incremental") {
    val input = TradeHistoryInput(market(1), sec(20))
    val trades = List(
      trade(sec(30), "A"),
      trade(sec(40), "B"),
      trade(sec(50), "C"),
    )
    val inputUpdates = stream(input, trades).map(_._2).toList
    inputUpdates(1).prev.get should be theSameInstanceAs inputUpdates(0)
    inputUpdates(2).prev.get should be theSameInstanceAs inputUpdates(1)
  }

}
