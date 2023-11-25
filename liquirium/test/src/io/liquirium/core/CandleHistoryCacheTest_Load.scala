package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.c10
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

class CandleHistoryCacheTest_Load extends CandleHistoryCacheTest {

  test("it requests the first candle start and last candle end from the store") {
    load(sec(100), sec(120))
    storeGetStartAndEnd.verifyTimes(1).getFirstStartAndLastEnd
  }

  test("if there are no boundaries (no candles) it requests an update with the full period") {
    load(sec(100), sec(120))
    storeGetStartAndEnd.completeNext(None)
    expectUpdateRequestResult(sec(100), sec(120))
  }

  test("if all requested candles are within the boundary, they are loaded and returned") {
    load(sec(100), sec(120))
    storeGetStartAndEnd.completeNext(Some((sec(90), sec(130))))
    storeGet.verify.get(Some(sec(100)), Some(sec(120)))
    completeStoreGet(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    expectHistoryResult(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
  }

  test("stored candles may match the requested boundaries exactly and they are immediately returned") {
    load(sec(100), sec(120))
    storeGetStartAndEnd.completeNext(Some((sec(100), sec(120))))
    storeGet.verify.get(Some(sec(100)), Some(sec(120)))
    completeStoreGet(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    expectHistoryResult(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
  }

  test("a proper update request is issued when the stored start and end are before the requested start") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(50), sec(90))
    expectUpdateRequestResult(sec(90), sec(150))
  }

  test("a proper update request is issued when stored start is before start and stored end at requested start") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(50), sec(100))
    expectUpdateRequestResult(sec(100), sec(150))
  }

  test("a proper update request is issued when stored start is before start and stored end before end") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(50), sec(120))
    expectUpdateRequestResult(sec(120), sec(150))
  }

  test("a proper update request is issued when the start matches but the stored end is before the end") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(100), sec(120))
    expectUpdateRequestResult(sec(120), sec(150))
  }

  test("a proper update request is issued when the stored period is within the requested period") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(110), sec(140))
    expectUpdateRequestResult(sec(100), sec(150))
  }

  test("a proper update request is issued when the stored start is after the start and the end matches") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(120), sec(150))
    expectUpdateRequestResult(sec(100), sec(120))
  }

  test("a proper update request is issued when the stored start is after the start and the stored end after the end") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(120), sec(170))
    expectUpdateRequestResult(sec(100), sec(120))
  }

  test("a proper update request is issued when the stored start is at the end") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(150), sec(170))
    expectUpdateRequestResult(sec(100), sec(150))
  }

  test("a proper update request is issued when the stored start is after the end") {
    load(sec(100), sec(150))
    provideStartAndEnd(sec(160), sec(170))
    expectUpdateRequestResult(sec(100), sec(160))
  }

}
