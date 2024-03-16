package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.helpers.CallingThreadExecutionContext
import io.liquirium.util.DummyLogger
import org.mockito.Mockito

import scala.concurrent.ExecutionContext

class BitfinexRestApiTest extends TestWithMocks {

  implicit val ec: ExecutionContext = CallingThreadExecutionContext

  val baseService = new FutureServiceMock[BitfinexHttpService, Any](_.sendRequest(*))

  val jsonConverter: BitfinexJsonConverter = mock[BitfinexJsonConverter]

  def api = new BitfinexRestApi(baseService.instance, jsonConverter, DummyLogger)

  def captureRequest(): BitfinexHttpRequest = {
    val adapterCaptor = argumentCaptor[BitfinexHttpRequest]
    baseService.verifyWithMode(Mockito.atLeastOnce()).sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

}
