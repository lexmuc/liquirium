package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime
import io.liquirium.bot.simulation.helpers.SimulationHelpers.makeChartDataSeriesConfig
import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.eval.Eval
import io.liquirium.eval.helpers.EvalHelpers.testEval
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper, thrownBy}

class ChartDataLoggerTest extends EvalBasedSimulationLoggerTest[ChartDataLogger] {

  implicit class ResultAccessors(vl: ChartDataLogger) {
    private def updates = vl.chartDataUpdates

    def candles: Iterable[Candle] = updates.map(_.candle)

    def dataPoints: Iterable[Map[Int, BigDecimal]] = updates.map(_.namedDataPoints)
  }

  var dataSeriesConfigsWithEvals: Seq[(ChartDataSeriesConfig, Eval[BigDecimal])] = Seq()

  private def addSeriesConfigWithEval(config: ChartDataSeriesConfig, eval: Eval[BigDecimal]): Unit = {
    dataSeriesConfigsWithEvals = dataSeriesConfigsWithEvals :+ (config, eval)
  }

  val candlesEval: Eval[CandleHistorySegment] = testEval(1)
  val mA: Eval[BigDecimal] = testEval(2)
  val mB: Eval[BigDecimal] = testEval(3)
  val mC: Eval[BigDecimal] = testEval(4)
  val mD: Eval[BigDecimal] = testEval(5)

  override def initialLogger(): ChartDataLogger =
    ChartDataLogger(
      candlesEval = candlesEval,
      dataSeriesConfigsWithEvals = dataSeriesConfigsWithEvals,
    )

  private def assertCandles(cc: Candle*) = {
    lastLoggingResult.get._1.get.candles shouldEqual cc
  }

  private def assertDataPoints(dp: Map[Int, BigDecimal]*) = {
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
    addSeriesConfigWithEval(makeChartDataSeriesConfig(123).copy(snapshotTime = SnapshotTime.CandleStart), mA)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(1))
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(2))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(3))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mA -> dec(5))

    assertDataPoints(
      Map(0 -> dec(1)),
      Map(0 -> dec(3)),
    )
  }

  test("end evals are taken at the end of the candle (beginning of next)") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))
    addSeriesConfigWithEval(makeChartDataSeriesConfig(123).copy(snapshotTime = SnapshotTime.CandleEnd), mB)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mB -> dec(1))
    fakeValuesAndLog(candlesEval -> candles0, mB -> dec(2))
    fakeValuesAndLog(candlesEval -> candles1, mB -> dec(3))
    fakeValuesAndLog(candlesEval -> candles1, mB -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mB -> dec(5))

    assertDataPoints(
      Map(0 -> dec(3)),
      Map(0 -> dec(5)),
    )
  }

  test("there can be several evals at the start and the end of the candle") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val candles2 = candles1.append(c5(sec(105), 1))

    addSeriesConfigWithEval(makeChartDataSeriesConfig(0).copy(snapshotTime = SnapshotTime.CandleStart), mA)
    addSeriesConfigWithEval(makeChartDataSeriesConfig(1).copy(snapshotTime = SnapshotTime.CandleStart), mB)
    addSeriesConfigWithEval(makeChartDataSeriesConfig(2).copy(snapshotTime = SnapshotTime.CandleEnd), mC)
    addSeriesConfigWithEval(makeChartDataSeriesConfig(3).copy(snapshotTime = SnapshotTime.CandleEnd), mD)

    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(1), mB -> dec(2), mC -> dec(0), mD -> dec(0))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(5), mB -> dec(6), mC -> dec(3), mD -> dec(4))
    fakeValuesAndLog(candlesEval -> candles2, mA -> dec(0), mB -> dec(0), mC -> dec(7), mD -> dec(8))

    assertDataPoints(
      Map(0 -> dec(1), 1 -> dec(2), 2 -> dec(3), 3 -> dec(4)),
      Map(0 -> dec(5), 1 -> dec(6), 2 -> dec(7), 3 -> dec(8)),
    )
  }

  test("the data series configs are made available via the logger") {
    val candles0 = candleHistorySegment(start = sec(100), candleLength = secs(5))()
    val candles1 = candles0.append(c5(sec(100), 1))
    val config0 = makeChartDataSeriesConfig(0)
    val config1 = makeChartDataSeriesConfig(1)
    addSeriesConfigWithEval(config0, mA)
    addSeriesConfigWithEval(config1, mB)
    initLogger()
    fakeValuesAndLog(candlesEval -> candles0, mA -> dec(1), mB -> dec(1))
    fakeValuesAndLog(candlesEval -> candles1, mA -> dec(1), mB -> dec(1))
    lastLogger.dataSeriesConfigs shouldEqual Seq(config0, config1)
  }

}
