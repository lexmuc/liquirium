package io.liquirium.core

import io.liquirium.core.CandleHistoryCache.IncoherentCandleHistoryException
import io.liquirium.core.helpers.CandleHelpers
import io.liquirium.core.helpers.CandleHelpers.c10
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.util.Failure

class CandleHistoryCacheTest_Extend extends CandleHistoryCacheTest {

  test("it requests the first candle start and last candle end from the store") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
    )
    cache.extendWith(segment)
    storeGetStartAndEnd.verifyTimes(1).getFirstStartAndLastEnd
  }

  test("if the store is empty the given candles are written to the store") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    cache.extendWith(segment)
    storeGetStartAndEnd.completeNext(None)
    storeAdd.verify.add(segment)
  }

  test("the update only returns when the candles have been written") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
    )
    val f = cache.extendWith(segment)
    storeGetStartAndEnd.completeNext(None)
    f.value.isDefined shouldBe false
    storeAdd.completeNext(())
    f.value.isDefined shouldBe true
  }

  test("if there is a stored start and end and no gap would be created, the candles are written to the store, too") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    cache.extendWith(segment)
    storeGetStartAndEnd.completeNext(Some((sec(80), sec(100))))
    storeAdd.verify.add(segment)
  }

  test("candles are not stored and it returns a failure when a gap before the stored candles would be created") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    val f = cache.extendWith(segment)
    storeGetStartAndEnd.completeNext(Some((sec(80), sec(90))))
    storeAdd.verifyNever.add(*)
    f.value.get shouldEqual Failure(IncoherentCandleHistoryException(sec(90), sec(100)))
  }

  test("candles are not stored and it returns a failure when a gap after the stored candles would be created") {
    val segment = CandleHelpers.candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    val f = cache.extendWith(segment)
    storeGetStartAndEnd.completeNext(Some((sec(130), sec(170))))
    storeAdd.verifyNever.add(*)
    f.value.get shouldEqual Failure(IncoherentCandleHistoryException(sec(120), sec(130)))
  }

}
