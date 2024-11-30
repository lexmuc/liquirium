package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.secs
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import org.mockito.Mockito.mock

import java.time.Duration
import scala.concurrent.Future

class CachingTradeHistoryLoaderTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val cache: TradeHistoryCache = mock(classOf[TradeHistoryCache])
  protected val cacheRead = new FutureServiceMock[
    TradeHistoryCache,
    Option[TradeHistorySegment],
  ](_.read(), Some(cache))
  protected val cacheExtend = new FutureServiceMock[TradeHistoryCache, Unit](_.extendWith(*), Some(cache))
  protected val cacheSave = new FutureServiceMock[TradeHistoryCache, Unit](_.save(*), Some(cache))

  protected val baseLoader = new FutureServiceMock[TradeHistoryLoader, TradeHistorySegment](_.loadHistory(*, *))
  protected var overlap: Duration = secs(0)

  protected def loader: CachingTradeHistoryLoader =
    new CachingTradeHistoryLoader(
      baseLoader = baseLoader.instance,
      cache = cache,
      overlap = overlap,
    )
  protected var resultFuture: Future[TradeHistorySegment] = _

}
