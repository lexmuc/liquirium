package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.helpers.{CallingThreadExecutionContext, DummyLogger}
import org.mockito.Mockito

class PoloniexRestApi_Test extends TestWithMocks {

  implicit val ec: CallingThreadExecutionContext.type = CallingThreadExecutionContext

  val httpService = new FutureServiceMock[PoloniexHttpService, Any](_.sendRequest(*))
  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  def api = new PoloniexRestApi(httpService.instance, jsonConverter, DummyLogger)

  def captureLastRequest(): PoloniexHttpRequest = {
    val adapterCaptor = argumentCaptor[PoloniexHttpRequest]
    httpService.verifyWithMode(Mockito.atLeastOnce()).sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

}
