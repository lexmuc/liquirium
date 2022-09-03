package io.liquirium.bot.simulation

import io.liquirium.bot.helpers.BotInputHelpers.timeInput
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

import java.time.{Duration, Instant}

class TimedInputUpdateStreamTest_TimeInput extends BasicTest {

  private def stream(resolution: Duration, start: Instant, end: Instant) =
    TimedInputUpdateStream.forTimeInput(timeInput(resolution), start = start, end = end)

  test("the stream starts at the next instant after the start that is divisible by the resolution") {
    stream(secs(10), sec(123), end=sec(200)).head shouldEqual(sec(130) -> sec(130))
    stream(secs(10), sec(130), end=sec(200)).head shouldEqual(sec(130) -> sec(130))
  }

  test("the stream continues according to the resolution") {
    stream(secs(10), sec(123), end=sec(200)).take(3).toList shouldEqual List(
      sec(130) -> sec(130),
      sec(140) -> sec(140),
      sec(150) -> sec(150),
    )
  }

  test("the stream ends when the next time would be greater than the end (inclusive end)") {
    // end is inclusive so the last simulation candle can still be processed
    stream(secs(10), sec(100), end=sec(120)).toList shouldEqual List(
      sec(100) -> sec(100),
      sec(110) -> sec(110),
      sec(120) -> sec(120),
    )
  }

}
