package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant
import scala.util.Success

class CachingTradeHistoryLoaderTest_WithEnd extends CachingTradeHistoryLoaderTest {

  protected def load(start: Instant, end: Instant): Unit = {
    resultFuture = loader.loadHistory(start, maybeEnd = Some(end))
  }

  test("it initially reads the cached segment") {
    load(sec(10), sec(12))
    cacheRead.verify.read()
  }

  test("if the cache is empty, it loads the whole period from the base loader, saves and then returns it truncated") {
    load(sec(10), sec(12))
    cacheRead.completeNext(None)
    baseLoader.verify.loadHistory(sec(10), maybeEnd = None)
    val loadedSegment = tradeHistorySegment(sec(10))(
      trade(sec(10), "A"),
      trade(sec(11), "B"),
      trade(sec(12), "C"),
      trade(sec(13), "D"),
    )
    baseLoader.completeNext(loadedSegment)
    cacheSave.verify.save(loadedSegment)
    resultFuture.isCompleted shouldBe false
    cacheSave.completeNext(())
    resultFuture.isCompleted shouldBe true
    val truncatedSegment = loadedSegment.truncate(sec(12))
    resultFuture.value.get shouldEqual Success(truncatedSegment)
  }

  test("if the cached segment start is after the start, it loads everything, saves and then returns it truncated") {
    load(sec(10), sec(12))
    cacheRead.completeNext(Some(
      tradeHistorySegment(sec(11))(trade(sec(12), "A"))
    ))
    baseLoader.verify.loadHistory(sec(10), maybeEnd = None)
    val loadedSegment = tradeHistorySegment(sec(10))(
      trade(sec(10), "A"),
      trade(sec(11), "B"),
      trade(sec(12), "C"),
      trade(sec(13), "D"),
    )
    baseLoader.completeNext(loadedSegment)
    cacheSave.verify.save(loadedSegment)
    resultFuture.isCompleted shouldBe false
    cacheSave.completeNext(())
    resultFuture.isCompleted shouldBe true
    val truncatedSegment = loadedSegment.truncate(sec(12))
    resultFuture.value.get shouldEqual Success(truncatedSegment)
  }

  test("if the cached segment start matches, it loads all trades from the loader from the segment end minus overlap") {
    overlap = secs(10)
    load(sec(10), sec(12))
    val cachedSegment = tradeHistorySegment(sec(10))(
      trade(sec(22), "A"),
      trade(sec(24), "B"),
    )
    cacheRead.completeNext(Some(cachedSegment))
    baseLoader.verify.loadHistory(sec(14), maybeEnd = None)
  }

  test("if the cached segment start is earlier, it also loads new trades from the segment end minus overlap") {
    overlap = secs(10)
    load(sec(17), sec(22))
    val cachedSegment = tradeHistorySegment(sec(10))(
      trade(sec(22), "A"),
      trade(sec(24), "B"),
    )
    cacheRead.completeNext(Some(cachedSegment))
    baseLoader.verify.loadHistory(sec(14), maybeEnd = None)
  }

  test("it does not load trades earlier than the cached segment start") {
    overlap = secs(10)
    load(sec(30), sec(40))
    cacheRead.completeNext(Some(
      tradeHistorySegment(sec(10))(
        trade(sec(12), "A"),
      )
    ))
    baseLoader.verify.loadHistory(sec(10), maybeEnd = None)
  }

  test("when a segment is cached and new trades are loaded, the cache is extended even beyond the given end") {
    overlap = secs(10)
    load(sec(10), sec(11))
    cacheRead.completeNext(Some(
      tradeHistorySegment(sec(10))(
        trade(sec(12), "A"),
      )
    ))
    val extensionSegment = tradeHistorySegment(sec(10))(
      trade(sec(12), "A"),
      trade(sec(14), "B"),
    )
    baseLoader.completeNext(extensionSegment)
    cacheExtend.verify.extendWith(extensionSegment)
  }

  test("only after the cache is extended, the extended segment is returned (but truncated)") {
    overlap = secs(1)
    load(sec(10), sec(16))
    val cachedSegment =
      tradeHistorySegment(sec(10))(
        trade(sec(12), "A"),
        trade(sec(14), "B"),
      )
    cacheRead.completeNext(Some(cachedSegment))
    val extensionSegment = tradeHistorySegment(sec(13))(
      trade(sec(14), "B"),
      trade(sec(15), "C"),
      trade(sec(16), "D"),
    )
    baseLoader.completeNext(extensionSegment)
    resultFuture.isCompleted shouldBe false
    cacheExtend.completeNext(())
    resultFuture.value.get shouldEqual Success(
      tradeHistorySegment(sec(10))(
        trade(sec(12), "A"),
        trade(sec(14), "B"),
        trade(sec(15), "C"),
      )
    )
  }

  test("when the cached segment starts earlier, it is still extended but a trimmed (extended) segment is returned") {
    overlap = secs(1)
    load(sec(14), sec(16))
    val cachedSegment =
      tradeHistorySegment(sec(10))(
        trade(sec(12), "A"),
        trade(sec(14), "B"),
      )
    cacheRead.completeNext(Some(cachedSegment))
    val extensionSegment = tradeHistorySegment(sec(13))(
      trade(sec(14), "B"),
      trade(sec(15), "C"),
      trade(sec(16), "D"),
    )
    baseLoader.completeNext(extensionSegment)
    cacheExtend.completeNext(())
    resultFuture.value.get shouldEqual Success(
      tradeHistorySegment(sec(14))(
        trade(sec(14), "B"),
        trade(sec(15), "C"),
      )
    )
  }

}
