package io.liquirium.connect.binance

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.helpers.{CallingThreadExecutionContext, DummyLogger}
import org.mockito.Mockito

class BinanceRestApiTest extends TestWithMocks {

  implicit val ec = CallingThreadExecutionContext

  val baseService = new FutureServiceMock[BinanceExtendedHttpService, Any](_.sendRequest(*))
  val jsonConverter = mock[BinanceJsonConverter]

  def api = new BinanceRestApi(baseService.instance, jsonConverter, DummyLogger)

  def captureRequest() = {
    val adapterCaptor = argumentCaptor[BinanceHttpRequest]
    baseService.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  def reset() = {
    Mockito.reset(jsonConverter)
    baseService.reset()
  }

}

