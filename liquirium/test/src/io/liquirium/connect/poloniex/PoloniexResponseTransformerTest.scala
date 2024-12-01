package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.util.HttpResponse
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.{JsNumber, JsObject}

import scala.util.{Failure, Success}

class PoloniexResponseTransformerTest extends BasicTest {

  private def transformSuccess(code: Int, body: String) =
    PoloniexResponseTransformer.apply(Success(HttpResponse(code, body)))

  private def transformFailure(t: Throwable) = PoloniexResponseTransformer.apply(Failure(t))

  test("a success is parsed as json") {
    transformSuccess(200, "{\"test\": 123}") shouldEqual Success(JsObject(Seq("test" -> JsNumber(123))))
  }

  test("it returns an error when the http request fails") {
    transformFailure(ex("fail")) shouldEqual Failure(PoloniexApiError("The http request failed", Some(ex("fail"))))
  }

  test("it returns an explicit error when the status in not 200 but the response contains a code and message") {
    transformSuccess(422, """{ "code": 21604, "message": "Invalid UserId" }""") should matchPattern {
      case Failure(ExplicitPoloniexApiError(21604, "Invalid UserId")) =>
    }
  }

  test("it returns an error when the status is not 200") {
    transformSuccess(500, """{ "valid":"json" }""") shouldEqual
      Failure(PoloniexApiError("""Received response with status 500: { "valid":"json" }""", None))
  }

  test("it returns an error when the response is not valid json") {
    transformSuccess(200, """{ "asdf" == jklö }""") should matchPattern {
      case Failure(OtherPoloniexApiError("""Response is not valid json: { "asdf" == jklö }""", Some(_))) =>
    }
  }

  test("an empty response yields a specific error") {
    transformSuccess(200, "") should equal(Failure(PoloniexApiError("Response was empty - status 200")))
    transformSuccess(401, "") should equal(Failure(PoloniexApiError("Response was empty - status 401")))
  }

  test("a non-JSON response with the string 'CloudFlare' in it yields an error message referring to CloudFlare") {
    transformSuccess(200, "cannot be parsed CloudFlare") should matchPattern {
      case Failure(OtherPoloniexApiError("Response could not be parsed. Looks like a CloudFlare error.", Some(_))) =>
    }
  }

  test("a non-JSON response containing '::CLOUDFLARE_ERROR' yields an error message referring to CloudFlare") {
    transformSuccess(200, "cannot be parsed ::CLOUDFLARE_ERROR") should matchPattern {
      case Failure(OtherPoloniexApiError("Response could not be parsed. Looks like a CloudFlare error.", Some(_))) =>
    }
  }

  test("a non-JSON response containing 'cloudflare-nginx' yields an error message referring to CloudFlare") {
    transformSuccess(200, "cannot be parsed cloudflare-nginx") should matchPattern {
      case Failure(OtherPoloniexApiError("Response could not be parsed. Looks like a CloudFlare error.", Some(_))) =>
    }
  }

  test("a response status 502 cointaining 'CLOUDFLARE_ERROR' yields an error message referring to CloudFlare") {
    transformSuccess(502, "some html containing CLOUDFLARE_ERROR") should matchPattern {
      case Failure(OtherPoloniexApiError("Received status code 502. Looks like a CloudFlare error.", None)) =>
    }
  }

}
