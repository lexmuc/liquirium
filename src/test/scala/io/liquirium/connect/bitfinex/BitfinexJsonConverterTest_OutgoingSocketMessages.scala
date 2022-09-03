package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexOrder.OrderType
import io.liquirium.connect.bitfinex.BitfinexOutMessage._
import io.liquirium.core.helpers.TestWithMocks
import play.api.libs.json.{JsString, JsValue, Json}

class BitfinexJsonConverterTest_OutgoingSocketMessages extends TestWithMocks {

  val converter = new BitfinexJsonConverter()

  private def convert(m: BitfinexOutMessage) = converter.convertOutgoingMessage(m)

  test("a cancel message becomes an 'oc' message on channel 0 with the correct id") {
    convert(CancelOrderMessage(123)) shouldEqual Json.parse("""[0,"oc",null,{"id":123}]""")
  }

  test("a new order message is an 'on' message on channel 0 with all relevant fields") {
    convert(PlaceOrderMessage(
      clientOrderId = 111,
      symbol = "tIOTBTC",
      orderType = OrderType.ExchangeLimit,
      amount = BigDecimal("0.33"),
      price = BigDecimal("4.44"),
      flags = Set(OrderFlag.PostOnly, OrderFlag.OCO)
    )) shouldEqual
      Json.parse(
        s"""
           |[
           |  0,
           |  "on",
           |  null,
           |  {
           |    "cid": 111,
           |    "type": ${ converter.convertOrderType(OrderType.ExchangeLimit).toString() },
           |    "symbol": "tIOTBTC",
           |    "amount": "0.33",
           |    "price": "4.44",
           |    "flags": ${ OrderFlag.PostOnly.intValue | OrderFlag.OCO.intValue}
           |  }
           |]
      """.stripMargin)
  }

  test("quantity and price in an order may be very small but are not sent in scientific notation") {
    val json = convert(PlaceOrderMessage(
      clientOrderId = 111,
      symbol = "tIOTBTC",
      orderType = OrderType.ExchangeLimit,
      amount = BigDecimal("0.00000003"),
      price = BigDecimal("0.00000004"),
      flags = Set(OrderFlag.PostOnly, OrderFlag.OCO)
    ))
    val innerObj = json.as[List[JsValue]].apply(3).as[Map[String, JsValue]]
    innerObj.apply("amount") shouldEqual JsString("0.00000003")
    innerObj.apply("price") shouldEqual JsString("0.00000004")
  }

  test("an auth message contains the given nonce, apiKey and is signed with the authenticator") {
    val authenticator = mock[BitfinexAuthenticator]
    authenticator.apiKey returns "key123"
    authenticator.sign("AUTH4711") returns "<SIGNED>"
    convert(AuthMessage(4711, authenticator)) shouldEqual Json.parse(
      """
        |{
        |  "event": "auth",
        |  "apiKey": "key123",
        |  "authNonce": 4711,
        |  "authPayload": "AUTH4711",
        |  "authSig": "<SIGNED>"
        |}
      """.stripMargin)
  }

  test("a ticker subscription is properly serialized") {
    convert(SubscribeToTickerMessage("fABCDEF")) shouldEqual Json.parse(
      """
        |{
        |  "event": "subscribe",
        |  "channel": "ticker",
        |  "symbol": "fABCDEF"
        |}
      """.stripMargin)
  }

  test("a channel unsubscribe message is properly serialized") {
    convert(UnsubscribeFromChannelMessage(1234)) shouldEqual Json.parse(
      """
        |{
        |  "event": "unsubscribe",
        |  "chanId": 1234
        |}
      """.stripMargin)
  }

}
