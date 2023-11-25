package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexOrder.OrderType
import io.liquirium.core.helpers.BasicTest
import play.api.libs.json.{JsString, JsValue}

class BitfinexJsonConverterTest_OrderTypes extends BasicTest {

  def convert(ot: OrderType): JsValue = new BitfinexJsonConverter().convertOrderType(ot)

  test("it converts all types to the respective strings") {
    convert(OrderType.Limit) shouldEqual JsString("LIMIT")
    convert(OrderType.ExchangeLimit) shouldEqual JsString("EXCHANGE LIMIT")
    convert(OrderType.Market) shouldEqual JsString("MARKET")
    convert(OrderType.ExchangeMarket) shouldEqual JsString("EXCHANGE MARKET")

    convert(OrderType.Stop) shouldEqual JsString("STOP")
    convert(OrderType.ExchangeStop) shouldEqual JsString("EXCHANGE STOP")
    convert(OrderType.StopLimit) shouldEqual JsString("STOP LIMIT")
    convert(OrderType.ExchangeStopLimit) shouldEqual JsString("EXCHANGE STOP LIMIT")
    convert(OrderType.TrailingStop) shouldEqual JsString("TRAILING STOP")
    convert(OrderType.ExchangeTrailingStop) shouldEqual JsString("EXCHANGE TRAILING STOP")
    convert(OrderType.Fok) shouldEqual JsString("FOK")
    convert(OrderType.ExchangeFok) shouldEqual JsString("EXCHANGE FOK")
    convert(OrderType.Ioc) shouldEqual JsString("IOC")
    convert(OrderType.ExchangeIoc) shouldEqual JsString("EXCHANGE IOC")
  }

}
