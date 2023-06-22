package io.liquirium.bot.simulation

import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.eval.Eval
import io.liquirium.eval.helpers.EvalHelpers.testEval

class VisualizationLoggerTest extends EvalBasedSimulationLoggerTest[VisualizationLogger] {

  implicit class ResultAccessors(vl: VisualizationLogger) {
    private def updates = vl.visualizationUpdates

    def candles: Iterable[Candle] = updates.map(_.candle)

    def dataPoints: Iterable[Map[String, BigDecimal]] = updates.map(_.namedDataPoints)
  }

  var candleStartEvals: Map[String, Eval[BigDecimal]] = Map()
  var candleEndEvals: Map[String, Eval[BigDecimal]] = Map()

  val candlesEval: Eval[CandleHistorySegment] = testEval(1)
  val mA: Eval[BigDecimal] = testEval(2)
  val mB: Eval[BigDecimal] = testEval(3)
  val mC: Eval[BigDecimal] = testEval(4)
  val mD: Eval[BigDecimal] = testEval(5)

  override def initialLogger(): VisualizationLogger =
    VisualizationLogger(
      candlesEval = candlesEval,
      candleStartEvals = candleStartEvals,
      candleEndEvals = candleEndEvals,
    )

  private def assertCandles(cc: Candle*) = {
    lastLoggingResult.get._1.get.candles shouldEqual cc
  }

  private def assertDataPoints(dp: Map[String, BigDecimal]*) = {
    lastLoggingResult.get._1.get.dataPoints shouldEqual dp
  }

  test("no candle is logged when the candle history is empty") {
    val candles = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    initLogger()
    fakeValues(candlesEval -> candles)
    log()
    assertCandles()
  }

  test("candles are updated when it is logged again") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    initLogger()
    fakeValues(candlesEval -> candles0)
    log()
    val candles1 = candles0.append(c5(sec(100), 1))
    fakeValues(candlesEval -> candles1)
    log()
    assertCandles(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))
    fakeValues(candlesEval -> candles2)
    log()
    assertCandles(c5(sec(100), 1), c5(sec(105), 1))
  }

  test("it throws an exception when the candles are not initially empty") {
    initLogger()
    fakeValues(candlesEval -> candleHistorySegment(c5(sec(100), 1)))
    a[RuntimeException] shouldBe thrownBy(log())
  }

  test("it throws an exception when there are several new candles at once") {
    initLogger()
    fakeValues(candlesEval -> candleHistorySegment(start = sec(100), candleLength = secs(5))())
    log()
    fakeValues(candlesEval -> candleHistorySegment(c5(sec(100), 1), c5(sec(105), 1)))
    a[RuntimeException] shouldBe thrownBy(log())
  }

  test("it throws an exception when the candles are not derived from the last candles") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    initLogger()
    fakeValues(candlesEval -> candles0)
    log()
    val candles1a = candles0.append(c5(sec(100), 1))
    fakeValues(candlesEval -> candles1a)
    log()

    val candles1b = candles0.append(c5(sec(100), 2))
    fakeValues(candlesEval -> candles1b)
    a[RuntimeException] shouldBe thrownBy(log())
  }

  test("start evals are taken at the beginning of the candle") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))
    candleStartEvals = Map("mA" -> mA)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(1))
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(2))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(3))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mA -> dec(5))

    assertDataPoints(
      Map("mA" -> dec(1)),
      Map("mA" -> dec(3)),
    )
  }

  test("end evals are taken at the end of the candle (beginning of next)") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))
    candleEndEvals = Map("mB" -> mB)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mB -> dec(1))
    fakeValuesAndLog(candlesEval -> candles0, mB -> dec(2))
    fakeValuesAndLog(candlesEval -> candles1, mB -> dec(3))
    fakeValuesAndLog(candlesEval -> candles1, mB -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mB -> dec(5))

    assertDataPoints(
      Map("mB" -> dec(3)),
      Map("mB" -> dec(5)),
    )
  }

  test("there can be several evals at the start and the end of the candle") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))
    candleStartEvals = Map("mA" -> mA, "mB" -> mB)
    candleEndEvals = Map("mC" -> mC, "mD" -> mD)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(1), mB -> dec(2), mC -> dec(0), mD -> dec(0))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(5), mB -> dec(6), mC -> dec(3), mD -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mA -> dec(0), mB -> dec(0), mC -> dec(7), mD -> dec(8))

    assertDataPoints(
      Map("mA" -> dec(1), "mB" -> dec(2), "mC" -> dec(3), "mD" -> dec(4)),
      Map("mA" -> dec(5), "mB" -> dec(6), "mC" -> dec(7), "mD" -> dec(8)),
    )
  }

}
