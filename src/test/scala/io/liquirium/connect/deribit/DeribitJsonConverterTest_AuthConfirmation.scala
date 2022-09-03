package io.liquirium.connect.deribit

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.millis
import play.api.libs.json.Json

class DeribitJsonConverterTest_AuthConfirmation extends BasicTest {

  private def convert(token: String = "", expiresIn: Long = 0, scope: String = "") = {
    val s = s"""
       |{
       |  "access_token": "$token",
       |  "expires_in": ${expiresIn.toString},
       |  "refresh_token": "unused",
       |  "scope": "$scope",
       |  "token_type": "unused"
       |}
     """.stripMargin
    new DeribitJsonConverter().convertAuthConfirmation(Json.parse(s))
  }

  test("the token value is extracted") {
    convert(token = "abc").token shouldEqual DeribitAccessToken("abc")
  }

  test("the expiresIn fields is interpreted as milliseconds and returned as a duration") {
    convert(expiresIn = 12345).expiresIn shouldEqual millis(12345)
  }

  test("the scope field is parsed to a set of strings") {
    convert(scope = "asdf jklö xxx").scopes shouldEqual Set("asdf", "jklö", "xxx")
  }

}
