package io.liquirium.connect.binance

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.helpers.CallingThreadExecutionContext
import io.liquirium.util.DummyLogger
import org.mockito.Mockito
import org.mockito.Mockito.mock

class BinanceRestApiTest extends TestWithMocks {

  implicit val ec: CallingThreadExecutionContext.type = CallingThreadExecutionContext

  val baseService = new FutureServiceMock[BinanceExtendedHttpService, Any](_.sendRequest(*))
  val jsonConverter: BinanceJsonConverter = mock(classOf[BinanceJsonConverter])

  def api = new BinanceRestApi(baseService.instance, jsonConverter, DummyLogger)

  def captureRequest(): BinanceHttpRequest = {
    val adapterCaptor = argumentCaptor[BinanceHttpRequest]
    baseService.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  def reset(): Unit = {
    Mockito.reset(jsonConverter)
    baseService.reset()
  }

}

