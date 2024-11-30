package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.util.HttpResponse
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.{JsNumber, JsObject}

import scala.util.{Failure, Success}

class CoinbaseResponseTransformerTest extends BasicTest {

  private def transformSuccess(code: Int, body: String) =
    CoinbaseResponseTransformer.apply(Success(HttpResponse(code, body)))

  private def transformFailure(t: Throwable) = CoinbaseResponseTransformer.apply(Failure(t))

  test("a success is parsed as json") {
    transformSuccess(200, "{\"test\": 123}") shouldEqual Success(JsObject(Seq("test" -> JsNumber(123))))
  }

  test("it returns an error when the http request fails") {
    transformFailure(ex("fail")) shouldEqual Failure(CoinbaseApiError("The http request failed", Some(ex("fail"))))
  }

  test("it returns an explicit error when the status in not 200 but the response contains a code and message") {
    transformSuccess(422, """{ "code": 21604, "message": "Invalid UserId" }""") should matchPattern {
      case Failure(ExplicitCoinbaseApiError(21604, "Invalid UserId")) =>
    }
  }

  test("it returns an error when the status is not 200") {
    transformSuccess(500, """{ "valid":"json" }""") shouldEqual
      Failure(CoinbaseApiError("""Received response with status 500: { "valid":"json" }""", None))
  }

  test("it returns an error when the response is not valid json") {
    transformSuccess(200, """{ "asdf" == jklö }""") should matchPattern {
      case Failure(OtherCoinbaseApiError("""Response is not valid json: { "asdf" == jklö }""", Some(_))) =>
    }
  }

  test("an empty response yields a specific error") {
    transformSuccess(200, "") should equal(Failure(CoinbaseApiError("Response was empty")))
  }

}
