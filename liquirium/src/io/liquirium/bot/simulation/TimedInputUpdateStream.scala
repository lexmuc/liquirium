package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput
import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.core.{Candle, CandleHistorySegment, Trade, TradeHistorySegment}

import java.time.Instant

object TimedInputUpdateStream {

  def forTradeHistory(
    thi: BotInput.TradeHistoryInput,
    trades: Iterable[Trade],
  ): LazyList[(Instant, TradeHistorySegment)] = {
    case class State(ths: TradeHistorySegment, rest: List[Trade]) {
      def next: State = {
        val nextTrades = rest.takeWhile(_.time == rest.head.time)
        val nextSegment = nextTrades.foldLeft(ths) { case (ths, trade) => ths.append(trade) }
        State(nextSegment, rest.drop(nextTrades.size))
      }
    }

    def stateStream(s: State): LazyList[State] = s #:: (if (s.rest.isEmpty) LazyList.empty else stateStream(s.next))

    val emptySegmentState = State(TradeHistorySegment.empty(thi.start), trades.toList)
    val initState = if (trades.headOption.map(_.time).contains(thi.start)) emptySegmentState.next
    else emptySegmentState
    stateStream(initState).map { state => state.ths.end -> state.ths }
  }

  def forCandleHistory(
    history: BotInput.CandleHistoryInput,
    candles: Iterable[Candle],
  ): LazyList[(Instant, CandleHistorySegment)] = {
    case class State(chs: CandleHistorySegment, rest: List[Candle]) {
      def next: State = State(chs.append(rest.head), rest.tail)
    }
    def stateStream(s: State): LazyList[State] = s #:: (if (s.rest.isEmpty) LazyList.empty else stateStream(s.next))

    val initState = State(CandleHistorySegment.empty(history.start, history.candleLength), candles.toList)
    stateStream(initState).map { state => state.chs.end -> state.chs }
  }

  def forTimeInput(timeInput: TimeInput, start: Instant, end: Instant): LazyList[(Instant, Instant)] = {
    val resolutionMillis = timeInput.resolution.toMillis
    val rest = start.toEpochMilli % resolutionMillis
    val firstTime =
      if (rest == 0) start
      else Instant.ofEpochMilli(start.toEpochMilli - rest + resolutionMillis)
    LazyList.iterate(firstTime)(t => Instant.ofEpochMilli(t.toEpochMilli + resolutionMillis))
      .takeWhile(!_.isAfter(end))
      .map(t => (t, t))
  }

}
