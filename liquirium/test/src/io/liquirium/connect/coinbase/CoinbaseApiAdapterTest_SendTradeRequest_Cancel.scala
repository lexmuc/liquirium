package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.CancelOrders
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseCancelOrderResult => ccor}
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.{CancelRequest, Market}

import scala.util.Failure

class CoinbaseApiAdapterTest_SendTradeRequest_Cancel extends CoinbaseApiAdapterTest {

  private def sendTradeRequest(
    market: Market = m("m"),
    orderId: String = "abc"
  ) = apiAdapter.sendTradeRequest(
    CancelRequest(
      market = market,
      orderId = orderId
    ))

  private def captureCancelOrderRequest(): CancelOrders = {
    val adapterCaptor = argumentCaptor[CancelOrders]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def replyWith(response: CoinbaseCancelOrderResult): Unit = fakeRestApi.completeNext(Seq(response))

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("a cancel request yields a cancel with the respective order id sequence") {
    sendTradeRequest(orderId = "efg")
    val req = captureCancelOrderRequest()
    req.orderIds shouldEqual Seq("efg")
  }

  test("an execption is thrown in case the returned cancel response was no success") {
    val f = sendTradeRequest()
    replyWith(ccor(success = false))
    f.value.get.isFailure shouldBe true
  }

  test("errors are passed along") {
    val f = sendTradeRequest()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
