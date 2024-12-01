package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexInMessage._
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{order, trade}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsNumber, Json}

class BitfinexMessageConverterTest extends TestWithMocks {

  val jsonConverter: BitfinexJsonConverter = mock(classOf[BitfinexJsonConverter])

  def convert(s: String): BitfinexInMessage =
    new BitfinexMessageConverter(jsonConverter).convertIncomingMessage(Json.parse(s))

  val defaultAuthCaps =
    """{
      |    "orders":{"read":1,"write":1},
      |    "account":{"read":1,"write":0},
      |    "funding":{"read":1,"write":0},
      |    "history":{"read":1,"write":0},
      |    "wallets":{"read":1,"write":0},
      |    "withdraw":{"read":0,"write":0},
      |    "positions":{"read":1,"write":0}
      |  }"""

  test("it recognizes an authentication info") {
    convert(
      s"""{
         |  "event":"auth",
         |  "status":"OK",
         |  "chanId":0,
         |  "userId":1091000,
         |  "auth_id":"19151ede-7f10-4e5f-8c19-beba3adce11d",
         |  "caps": $defaultAuthCaps
         |}""".stripMargin) shouldEqual AuthConfirmation()
  }

  test("it returns a failed authentication when the status is not 'OK'") {
    convert(
      s"""{
         |  "event":"auth",
         |  "status":"NOT_OK_WTF?",
         |  "chanId":0,
         |  "userId":1091000,
         |  "auth_id":"19151ede-7f10-4e5f-8c19-beba3adce11d",
         |  "caps": $defaultAuthCaps
         |}""".stripMargin) shouldEqual AuthFailure()
  }

  test("it parses error messages with message and code") {
    convert("""{"event":"error","msg":"auth: invalid","code":10100}""") shouldEqual
      ErrorMessage("auth: invalid", 10100)
  }

  test("it recognizes an order state message and parses it properly") {
    jsonConverter.convertOrders(json(123)) returns Seq(order(1), order(2))
    convert(s"""[0, "os", ${ json(123) }]""") shouldEqual OrderStateMessage(Seq(order(1), order(2)))
  }

  test("temporary fiddle. REMOVE!") {
    val arr = Json.parse(s"""[0, "os", ${ json(123) }]""").asInstanceOf[JsArray]
    arr.value.toSeq.take(1) match {
      case Seq(x: JsNumber) => println("Is number")
    }
  }

  test("it recognizes a new-trade order") {
    jsonConverter.convertSingleTrade(json(123)) returns trade(123)
    convert(s"""[0, "tu",${ json(123) }]""") shouldEqual NewTradeMessage(trade(123))
  }

  test("it recognizes a cancel message independent of status") {
    val o123 = order(123, status = BitfinexOrder.OrderStatus.Canceled)
    jsonConverter.convertSingleOrder(json(123)) returns o123
    convert(s"""[0, "oc",${ json(123) }]""") shouldEqual OrderCancelMessage(o123)
    val o234 = order(234, status = BitfinexOrder.OrderStatus.Executed)
    jsonConverter.convertSingleOrder(json(234)) returns o234
    convert(s"""[0, "oc",${ json(234) }]""") shouldEqual OrderCancelMessage(o234)
  }

  test("it recognizes an order update message") {
    jsonConverter.convertSingleOrder(json(123)) returns order(123)
    convert(s"""[0, "ou",${ json(123) }]""") shouldEqual OrderUpdateMessage(order(123))
  }

  test("it recognizes a cancel failure with the given order id") {
    val orderStub =
      s"""[1111,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
         |0,0,null,null,null,0,null,null,null,null,null,null,null,null]""".stripMargin
    val json = s"""[0,"n",[1563192251873,"oc-req",null,null,$orderStub,null,"ERROR","Order not found."]]"""
    convert(json) shouldEqual CancelFailureMessage(1111)
  }

  test("a cancel request success notification is considered an irrelevant message") {
    val order =
      s"""[28388304746,null,1,"tBTCUSD",1563192029512,1563192029516,0.1,0.1,"LIMIT",null,null,null,0,"ACTIVE"
         |,null,null,10000,0,0,0,null,null,null,0,0,null,null,null,"API>BFX",null,null,null]""".stripMargin
    val json =
      s"""[0,"n",[1563192221080,"oc-req",null,null,$order,null,"SUCCESS",
         |"Submitted for cancellation; waiting for confirmation (ID: 28388304746)."]]""".stripMargin
    convert(json) shouldEqual IrrelevantMessage(Json.parse(json))
  }

  test("it recognizes a new order message") {
    jsonConverter.convertSingleOrder(json(123)) returns order(123)
    convert(s"""[0, "on",${ json(123) }]""") shouldEqual NewOrderMessage(order(123))
  }

  test("it recognizes an order request ERROR with client order id and error message") {
    val orderStub =
      s"""[null,null,123,"tXMRBTC",null,null,-21.12309093999641,null,"LIMIT",null,null,null,null,null,null,null,
         |0.011172171662940399,null,0,0,null,null,null,0,null,null,null,null,null,null,null,null]""".stripMargin
    val jsonString = s"""[0,"n",[123123123,"on-req",null,null, $orderStub, null,"ERROR","message-123"]]"""
    convert(jsonString) shouldEqual OrderRequestFailureMessage(123, "message-123", symbol = "tXMRBTC")
  }

  test("it recognizes and properly converts a successful order request notification") {
    val jsonString =
      s"""[0,"n",[123123123,"on-req",null,null, ${ json(123).toString }, null,"SUCCESS","message-123"]]"""
    jsonConverter.convertSingleOrder(json(123)) returns order(123)
    convert(jsonString) shouldEqual SuccessfulOrderRequestNotification(order(123))
  }

  test("a channel ticker subscribed message is parsed properly with symbol and channel id") {
    convert(
      """ {
        |   "event": "subscribed",
        |   "channel": "ticker",
        |   "chanId": 123,
        |   "pair": "BTCUSD"
        |}""".stripMargin) shouldEqual TickerSubscribedMessage(123, "BTCUSD")
  }

  test("non-ticker subscribed messages are considered irrelevant") {
    val json =
      """ {
        |   "event": "subscribed",
        |   "channel": "wtf",
        |   "what": "ever"
        |}""".stripMargin
    convert(json) shouldEqual IrrelevantMessage(Json.parse(json))
  }

  test("a non-zero channel message is parsed as a general channel message with channel id and payload") {
    convert("""[123, { "as": "df" }]""") shouldEqual GeneralChannelMessage(123, Json.parse("""{"as": "df"}"""))
  }

  test("a general channel message for a ticker can be converted to a ticker message") {
    convert(
      """[123, [1.0,
        |    1.0,
        |    1.0,
        |    1.0,
        |    1.0,
        |    0.5,
        |    1.23,
        |    2.34,
        |    3.0,
        |    0.5]]""".stripMargin).asInstanceOf[GeneralChannelMessage].asTickerMessage shouldEqual
      TickerMessage(BigDecimal("1.23"))
  }

  test("any other weird notifications are considered irrelevant") {
    val json = s"""[0,"n","asdf"]"""
    convert(json) shouldEqual IrrelevantMessage(Json.parse(json))
  }

  test("any other channel zero messages are considered irrelevant") {
    val jsonA = s"""[0,"asdf"]"""
    convert(jsonA) shouldEqual IrrelevantMessage(Json.parse(jsonA))
    val jsonB = s"""[0,"asdf", "xxx"]"""
    convert(jsonB) shouldEqual IrrelevantMessage(Json.parse(jsonB))
  }

  test("random objects are passed as irrelevant messages containing the json") {
    val jsonString = """{"some": "irrelevant", "message": 0}"""
    convert(jsonString) shouldEqual IrrelevantMessage(Json.parse(jsonString))
  }

  test("a heartbeat message is recognized with channel id") {
    convert("""[0,"hb"]""") shouldEqual HeartbeatMessage(0)
    convert("""[123,"hb"]""") shouldEqual HeartbeatMessage(123)
  }

  test("random arrays are passed as irrelevant messages containing the json") {
    val jsonString = """["some", "irrelevant", "message"]"""
    convert(jsonString) shouldEqual IrrelevantMessage(Json.parse(jsonString))
  }

}
