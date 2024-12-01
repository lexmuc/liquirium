package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.helpers.JsonTestHelper.json
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}
import play.api.libs.json.{JsString, JsValue, Json}

class BitfinexRestApiTest_SubmitOrder extends BitfinexRestApiTest {

  private def fakeTypeConversion(`type`: BitfinexOrder.OrderType, jsValue: JsValue) = {
    jsonConverter.convertOrderType(`type`) returns jsValue
  }

  private def fakeConversion(json: JsValue, order: BitfinexOrder) = {
    jsonConverter.convertSingleOrder(json) returns order
  }

  private var lastRequest: BitfinexRestApi.SubmitOrder = _

  private def createOrder(
    `type`: BitfinexOrder.OrderType = BitfinexOrder.OrderType.Limit,
    symbol: String = "BTC-USD",
    price: Option[BigDecimal] = None,
    amount: BigDecimal = dec(1),
    flags: Set[BitfinexOrderFlag] = Set(),
  ): BitfinexRestApi.SubmitOrder = {
    lastRequest = BitfinexRestApi.SubmitOrder(
      `type` = `type`,
      symbol = symbol,
      price = price,
      amount = amount,
      flags = flags
    )
    lastRequest
  }

  private def assertParam(name: String, value: String): Unit = {
    lastRequest.httpRequest(jsonConverter).params should contain(name, value)
  }

  fakeTypeConversion(BitfinexOrder.OrderType.Limit, JsString("XXX"))

  test("the http request is a private POST request") {
    createOrder().httpRequest(jsonConverter) shouldBe a[PrivateBitfinexPostRequest]
  }

  test("the http request has the correct path") {
    createOrder().httpRequest(jsonConverter).lastPathSegment shouldEqual "auth/w/order/submit"
  }

  test("the type is encoded in the parameters") {
    fakeTypeConversion(BitfinexOrder.OrderType.StopLimit, JsString("STOP LIMIT"))
    createOrder(`type` = BitfinexOrder.OrderType.StopLimit)
    assertParam("type", "STOP LIMIT")
  }

  test("the symbol is encoded in the parameters") {
    createOrder(symbol = "ETH-EUR")
    assertParam("symbol", "ETH-EUR")
  }

  test("the price is encoded in the parameters") {
    createOrder(price = Option(dec("1.23")))
    assertParam("price", "1.23")
  }

  test("the amount is encoded in the parameters") {
    createOrder(amount = dec("1.1"))
    assertParam("amount", "1.1")
  }

  test("the flags are encoded in the parameters") {
    createOrder(flags = Set(BitfinexOrderFlag.Hidden))
    assertParam("flags", "64")

    val flags2: Set[BitfinexOrderFlag] = Set(BitfinexOrderFlag.Hidden, BitfinexOrderFlag.Close)
    createOrder(flags = flags2)
    assertParam("flags", "576")

    val allFlags: Set[BitfinexOrderFlag] = Set(
      BitfinexOrderFlag.Hidden,
      BitfinexOrderFlag.Close,
      BitfinexOrderFlag.OCO,
      BitfinexOrderFlag.PostOnly,
      BitfinexOrderFlag.NoVarRates,
      BitfinexOrderFlag.ReduceOnly
    )
    createOrder(flags = allFlags)
    assertParam("flags", "546368")
  }

  test("the response is converted to an order") {
    val response = Json.parse(
      s"""
         |[1567590617.442,
         |"on-req",
         |null,
         |null,
         |[${ json(123).toString() }],
         |null,
         |"SUCCESS",
         |"Submitting 1 orders."]
         |""".stripMargin)
    fakeConversion(json(123), BitfinexTestHelpers.order(123))
    createOrder().convertResponse(response, jsonConverter) shouldEqual BitfinexTestHelpers.order(123)
  }

}
