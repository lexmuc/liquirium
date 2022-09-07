package io.liquirium.core

import io.liquirium.core.helper.CandleHelpers.{c10, c5, e5}
import io.liquirium.core.helper.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_FromForwardCandles extends CandleHistorySegmentTest {

  test("it can be created from a forward candle iterable and exposes candles in reverse order") {
    val chs = fromForwardCandles(start = sec(10), resolution = secs(5))(
      c5(sec(10), 1),
      c5(sec(15), 2),
    )
    chs.reverseCandles shouldEqual List(
      c5(sec(15), 2),
      c5(sec(10), 1),
    )
  }

  test("it exposes start and resolution too when created from a forward candle iterable") {
    val chs = fromForwardCandles(start = sec(10), resolution = secs(5))(
      c5(sec(10), 1),
      c5(sec(15), 2),
    )
    chs.start shouldEqual sec(10)
    chs.resolution shouldEqual secs(5)
  }

  test("when constructing from forward candles gaps are filled with empty candles and start is as given") {
    val chs = fromForwardCandles(start = sec(10), resolution = secs(5))(
      c5(sec(20), 1),
      c5(sec(30), 2),
    )
    chs.reverseCandles shouldEqual List(
      c5(sec(30), 2),
      e5(sec(25)),
      c5(sec(20), 1),
      e5(sec(15)),
      e5(sec(10)),
    )
  }

  test("the end is determined by the last candle end if not given") {
    val chs = fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      end = None,
    )(
      c5(sec(10), 1),
      c5(sec(15), 2),
    )
    chs.end shouldEqual sec(20)
  }

  test("if an end is given the segment is padded with empty candles respectively") {
    val chs = fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      end = Some(sec(25)),
    )(
      c5(sec(10), 1),
    )
    chs.reverseCandles shouldEqual List(
      e5(sec(20)),
      e5(sec(15)),
      c5(sec(10), 1),
    )
    chs.end shouldEqual sec(25)
  }

  test("an empty sequence of candles can be padded, too") {
    val chs = fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      end = Some(sec(20)),
    )()
    chs.reverseCandles shouldEqual List(
      e5(sec(15)),
      e5(sec(10)),
    )
    chs.end shouldEqual sec(20)
  }

  test("the segment is not padded if not required") {
    val chs = fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      end = Some(sec(15)),
    )(
      c5(sec(10), 1),
    )
    chs.reverseCandles shouldEqual List(
      c5(sec(10), 1),
    )
  }

  test("it throws an exception when candles are not in order") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(start = sec(10), resolution = secs(5))(
        c5(sec(15), 1),
        c5(sec(10), 2),
      )
    }
  }

  test("it throws an exception when candles do not have the proper length") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(start = sec(10), resolution = secs(5))(
        c5(sec(10), 1),
        c10(sec(15), 2),
      )
    }
  }

  test("it throws an exception when candle slots are not properly aligned") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(start = sec(10), resolution = secs(5))(
        c5(sec(10), 1),
        c5(sec(16), 2),
      )
    }
  }

  test("it throws an exception when the end is not properly aligned") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(
        start = sec(10),
        resolution = secs(5),
        end = Some(sec(16)),
      )(
        c5(sec(10), 1),
      )
    }
  }

  test("it throws an exception when the end is earlier than the start") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(
        start = sec(10),
        resolution = secs(5),
        end = Some(sec(5)),
      )()
    }
  }

  test("it throws an exception when the end is earlier than the last candle end") {
    an[Exception] shouldBe thrownBy {
      fromForwardCandles(
        start = sec(10),
        resolution = secs(5),
        end = Some(sec(15)),
      )(
        c5(sec(10), 1),
        c5(sec(15), 1),
      )
    }
  }

}