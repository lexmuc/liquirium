package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import play.api.libs.json.{JsValue, Json}

class BitfinexJsonConverterTest_PairInfo extends BasicTest {

  private def convert(json: JsValue) = new BitfinexJsonConverter().convertPairInfo(json)

  private def infoJson(
    pair: String = "ABC",
    minOrderSize: String = "1",
    maxOrderSize: String = "1",
  ) = Json.parse(
    s"""
       |[
       |"$pair",
       |[
       |null,
       |null,
       |null,
       |"$minOrderSize",
       |"$maxOrderSize",
       |null,
       |null,
       |null,
       |null,
       |null,
       |null,
       |null
       |]
       |]
       |""".stripMargin
  )

  test("it parses the pair field") {
    convert(infoJson(pair = "ANTUSD")).pair shouldEqual "ANTUSD"
  }

  test("it parses the min order size field") {
    convert(infoJson(minOrderSize = "1.2")).minOrderSize shouldEqual dec("1.2")
  }

  test("it parses the max order size field") {
    convert(infoJson(maxOrderSize = "2.3")).maxOrderSize shouldEqual dec("2.3")
  }

}
