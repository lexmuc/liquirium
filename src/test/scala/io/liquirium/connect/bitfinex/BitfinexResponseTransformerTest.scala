package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsNumber, JsObject}

import scala.util.{Failure, Success}

class BitfinexResponseTransformerTest extends BasicTest {

  private def transformSuccess(code: Int, body: String) =
    BitfinexResponseTransformer.apply(Success(HttpResponse(code, body)))

  private def transformFailure(t: Throwable) = BitfinexResponseTransformer.apply(Failure(t))

  test("a success is parsed as json") {
    transformSuccess(200, "{\"test\": 123}") shouldEqual Success(JsObject(Seq("test" -> JsNumber(123))))
  }

  test("it returns an error when the http request fails") {
    transformFailure(ex("fail")) shouldEqual Failure(BitfinexApiError("The http request failed", Some(ex("fail"))))
  }

  test("it returns an explicit error when the status is not 200") {
    transformSuccess(500, """{ "valid":"json" }""") shouldEqual
      Failure(ExplicitBitfinexApiError("""Received response with status 500: { "valid":"json" }""", None))
  }

  test("it returns an error when the response is not valid json") {
    transformSuccess(200, """{ "asdf" == jklö }""") should matchPattern {
      case Failure(OtherBitfinexApiError("""Response is not valid json: { "asdf" == jklö }""", Some(_))) =>
    }
  }

  test("an empty response yields a specific error") {
    transformSuccess(200, "") should equal(Failure(BitfinexApiError("Response was empty")))
  }

}
