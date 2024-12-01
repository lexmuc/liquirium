package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.PostWithApiKey
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.Json

import scala.concurrent.Future

class BinanceRestApiTest_CreateListenKey extends BinanceRestApiTest {


  def createListenKey(margin: Boolean = false): Future[String] =
    api.sendRequest(BinanceRestApi.CreateListenKey(margin))

  test("it sends a private post request to the correct endpoint depending on whether it is a margin key or not") {
    createListenKey(margin = false)
    captureRequest() should matchPattern { case PostWithApiKey("/api/v3/userDataStream", _) => }

    reset()

    createListenKey(margin = true)
    captureRequest() should matchPattern { case PostWithApiKey("/sapi/v3/userDataStream", _) => }
  }

  test("the listenKey is extracted from the response") {
      val f = createListenKey()
      baseService.completeNext(Json.parse("{ \"listenKey\": \"ASDF123\" }"))
      f.value.get.get shouldEqual "ASDF123"
  }

}
