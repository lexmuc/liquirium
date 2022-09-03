package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.helpers.{CallingThreadExecutionContext, DummyLogger}
import org.mockito.Mockito

class CoinbaseRestApiTest extends TestWithMocks {

  implicit val ec: CallingThreadExecutionContext.type = CallingThreadExecutionContext

  val httpService = new FutureServiceMock[CoinbaseHttpService, Any](_.sendRequest(*))
  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  def api = new CoinbaseRestApi(httpService.instance, jsonConverter, DummyLogger)

  def captureLastRequest(): CoinbaseHttpRequest = {
    val adapterCaptor = argumentCaptor[CoinbaseHttpRequest]
    httpService.verifyWithMode(Mockito.atLeastOnce()).sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

}
