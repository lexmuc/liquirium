package io.liquirium.connect.binance

import io.liquirium.core.helper.CoreHelpers.ex
import io.liquirium.core.helper.TestWithMocks
import io.liquirium.core.helper.async.FutureServiceMock
import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsNumber, JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class BinanceExtendedHttpServiceTest extends TestWithMocks {

  implicit val ec: ExecutionContext = io.liquirium.helper.CallingThreadExecutionContext

  val baseService = new FutureServiceMock[BinanceHttpService, HttpResponse](_.sendRequest(*))

  protected def sendRequest(r: BinanceHttpRequest = BinanceHttpRequest.PublicGet("", Seq())): Future[JsValue] =
    new BinanceExtendedHttpService(baseService.instance).sendRequest(r)

  protected def completeWithResponse(status: Int, body: String): Unit =
    baseService.completeNext(HttpResponse(status, body))

  test("it just fowards the request to the base service") {
    sendRequest(BinanceHttpRequest.PublicGet("123", Seq()))
    baseService.verify.sendRequest(BinanceHttpRequest.PublicGet("123", Seq()))
  }

  test("it parses a success json response and returns it") {
    val f = sendRequest()
    completeWithResponse(200, "{\"test\": 123}")
    f.value.get shouldEqual Success(JsObject(Seq("test" -> JsNumber(123))))
  }

  test("it returns an error when the response does contain invalid json") {
    val f = sendRequest()
    completeWithResponse(200, "{\"test\": 123")
    f.value.get should matchPattern {
      case Failure(BinanceApiError("Failed to parse response as json", None, Some(_))) =>
    }
  }

  test("it returns an error with error code and message when the failure response looks like an error") {
    val f = sendRequest()
    completeWithResponse(123, """{"code":-10020,"msg": "Sum ting wong"}""")
    f.value.get shouldEqual Failure(BinanceApiError("Sum ting wong", Some(-10020)))
  }

  test("it returns an error when the http request fails") {
    val f = sendRequest()
    baseService.failNext(ex("fail"))
    f.value.get shouldEqual Failure(BinanceApiError("The http request failed", None, Some(ex("fail"))))
  }

  test("it returns an error when an unexpected status code is received") {
    val f = sendRequest()
    completeWithResponse(123, "weird")
    f.value.get shouldEqual Failure(BinanceApiError("Received response with status 123: weird"))
  }

}
