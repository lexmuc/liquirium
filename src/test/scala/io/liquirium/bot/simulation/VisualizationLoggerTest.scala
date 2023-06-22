package io.liquirium.bot.simulation

import io.liquirium.core.Candle
import io.liquirium.core.helpers.CandleHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.dec
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

  val candleEval: Eval[Option[Candle]] = testEval(1)
  val mA: Eval[BigDecimal] = testEval(2)
  val mB: Eval[BigDecimal] = testEval(3)
  val mC: Eval[BigDecimal] = testEval(4)
  val mD: Eval[BigDecimal] = testEval(5)

  override def initialLogger(): VisualizationLogger =
    VisualizationLogger(
      latestCandle = candleEval,
      candleStartEvals = candleStartEvals,
      candleEndEvals = candleEndEvals,
    )

  private def fakeEvaluationsAndLogWithoutCandle(tuples: (Eval[_], _)*): Unit = {
    val candleTuple = candleEval -> None
    runLogWithEvaluations(candleTuple +: tuples)
  }

  private def fakeEvaluationsAndLog(candle: Candle, tuples: (Eval[_], _)*): Unit = {
    val candleTuple = candleEval -> Some(candle)
    runLogWithEvaluations(candleTuple +: tuples)
  }

  private def assertCandles(cc: Candle*) = {
    lastLoggingResult.get._1.get.candles shouldEqual cc
  }

  private def assertDataPoints(dp: Map[String, BigDecimal]*) = {
    lastLoggingResult.get._1.get.dataPoints shouldEqual dp
  }

  test("no candle is logged when the candle evaluation yields None") {
    initLogger()
    fakeEvaluationsAndLogWithoutCandle()
    assertCandles()
  }

  test("no candle is logged when the first evaluation yields a candle") {
    initLogger()
    fakeEvaluationsAndLog(candle(1))
    assertCandles()
  }

  test("the first candle is appended when the candle eval changes") {
    initLogger()
    fakeEvaluationsAndLog(candle(1))
    fakeEvaluationsAndLog(candle(1))
    assertCandles()
    fakeEvaluationsAndLog(candle(2))
    assertCandles(candle(2))

    initLogger()

    fakeEvaluationsAndLogWithoutCandle()
    fakeEvaluationsAndLogWithoutCandle()
    assertCandles()
    fakeEvaluationsAndLog(candle(2))
    assertCandles(candle(2))
  }

  test("further candles are only appended when the last candle eval changes") {
    initLogger()
    fakeEvaluationsAndLog(candle(1))
    fakeEvaluationsAndLog(candle(2))
    fakeEvaluationsAndLog(candle(2))
    assertCandles(candle(2))
    fakeEvaluationsAndLog(candle(3))
    assertCandles(candle(2), candle(3))
  }

  test("start evals are taken at the beginning of the candle") {
    candleStartEvals = Map("mA" -> mA)
    initLogger()
    fakeEvaluationsAndLog(candle(1), mA -> dec(1))
    fakeEvaluationsAndLog(candle(1))
    fakeEvaluationsAndLog(candle(2), mA -> dec(3))
    fakeEvaluationsAndLog(candle(2))
    fakeEvaluationsAndLog(candle(3), mA -> dec(5))

    assertDataPoints(
      Map("mA" -> dec(1)),
      Map("mA" -> dec(3)),
    )
  }

  test("end evals are taken at the end of the candle (beginning of next)") {
    candleEndEvals = Map("mB" -> mB)
    initLogger()
    fakeEvaluationsAndLog(candle(1), mB -> dec(0))
    fakeEvaluationsAndLog(candle(1))
    fakeEvaluationsAndLog(candle(2), mB -> dec(3))
    fakeEvaluationsAndLog(candle(2))
    fakeEvaluationsAndLog(candle(3), mB -> dec(5))

    assertDataPoints(
      Map("mB" -> dec(3)),
      Map("mB" -> dec(5)),
    )
  }

  test("there can be several evals at the start and the end of the candle") {
    candleStartEvals = Map("mA" -> mA, "mB" -> mB)
    candleEndEvals = Map("mC" -> mC, "mD" -> mD)
    initLogger()
    fakeEvaluationsAndLog(candle(1), mA -> dec(1), mB -> dec(2), mC -> dec(0), mD -> dec(0))
    fakeEvaluationsAndLog(candle(1))
    fakeEvaluationsAndLog(candle(2), mC -> dec(3), mD -> dec(4), mA -> dec(5), mB -> dec(6))
    fakeEvaluationsAndLog(candle(2))
    fakeEvaluationsAndLog(candle(3), mC -> dec(7), mD -> dec(8), mA -> dec(100), mB -> dec(100))

    assertDataPoints(
      Map("mA" -> dec(1), "mB" -> dec(2), "mC" -> dec(3), "mD" -> dec(4)),
      Map("mA" -> dec(5), "mB" -> dec(6), "mC" -> dec(7), "mD" -> dec(8)),
    )
  }

}
