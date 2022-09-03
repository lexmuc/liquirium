package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput
import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.Instant

object TimedInputUpdateStream {

  def forCandleHistory(
    history: BotInput.CandleHistoryInput,
    candles: Iterable[Candle],
  ): Stream[(Instant, CandleHistorySegment)] = {
    case class State(chs: CandleHistorySegment, rest: List[Candle]) {
      def next: State = State(chs.append(rest.head), rest.tail)
    }
    def stateStream(s: State): Stream[State] = s #:: (if (s.rest.isEmpty) Stream.empty else stateStream(s.next))

    val initState = State(CandleHistorySegment.empty(history.start, history.candleLength), candles.toList)
    stateStream(initState).map { state => state.chs.end -> state.chs }
  }

  def forTimeInput(timeInput: TimeInput, start: Instant, end: Instant): Stream[(Instant, Instant)] = {
    val resolutionMillis = timeInput.resolution.toMillis
    val rest = start.toEpochMilli % resolutionMillis
    val firstTime =
      if (rest == 0) start
      else Instant.ofEpochMilli(start.toEpochMilli - rest + resolutionMillis)
    Stream.iterate(firstTime)(t => Instant.ofEpochMilli(t.toEpochMilli + resolutionMillis))
      .takeWhile(!_.isAfter(end))
      .map(t => (t, t))
  }

}
