package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexHttpRequest.{PrivateDelete, PrivateGet, PrivatePost, PublicGet}
import io.liquirium.core.Side
import io.liquirium.util.akka.AsyncRequest
import play.api.libs.json.JsValue

import java.time.Instant

sealed trait PoloniexApiRequest[T] extends AsyncRequest[T] {

  def httpRequest: PoloniexHttpRequest

  def convertResponse(json: JsValue, converter: PoloniexJsonConverter): T

  def convertFailure(e: Throwable): Throwable = e

}

object PoloniexApiRequest {

  case class GetCandles(
    symbol: String,
    interval: PoloniexCandleLength,
    limit: Option[Int],
    startTime: Option[Instant],
    endTime: Option[Instant],
  ) extends PoloniexApiRequest[Seq[PoloniexCandle]] {

    override def httpRequest: PoloniexHttpRequest = {
      val params = (Seq("interval" -> interval.code)
        ++ limit.map(x => ("limit", x.toString))
        ++ startTime.map(x => ("startTime", x.toEpochMilli.toString))
        ++ endTime.map(x => ("endTime", x.toEpochMilli.toString))
        )
      PublicGet(s"/markets/$symbol/candles", params)
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): Seq[PoloniexCandle] =
      converter.convertCandles(json)

  }

  case class GetTradeHistory(
    limit: Option[Int],
    endTime: Option[Instant],
    startTime: Option[Instant],
    from: Option[Long], //Globally unique tradeid (use pageId value from response)
    direction: Option[String], //PRE, NEXT The direction before or after ‘from'
    symbols: List[String] = List(),
  ) extends PoloniexApiRequest[Seq[PoloniexTrade]] {

    override def httpRequest: PoloniexHttpRequest = {
      val symbolsParam = if (symbols.nonEmpty) Seq(("symbols", symbols.mkString(","))) else Seq()
      val params = (Seq()
        ++ limit.map(x => ("limit", x.toString))
        ++ startTime.map(x => ("startTime", x.toEpochMilli.toString))
        ++ endTime.map(x => ("endTime", x.toEpochMilli.toString))
        ++ from.map(x => ("from", x.toString))
        ++ direction.map(x => ("direction", x))
        ++ symbolsParam
        )
      PrivateGet("/trades", params)
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): Seq[PoloniexTrade] = {
      converter.convertTrades(json)
    }

  }

  case class GetOpenOrders(
    symbol: Option[String],
    side: Option[Side],
    from: Option[Long],
    direction: Option[String],
    limit: Option[Int],
  ) extends PoloniexApiRequest[Seq[PoloniexOrder]] {

    override def httpRequest: PoloniexHttpRequest = {
      val params = (Seq()
        ++ symbol.map(x => ("symbol", x))
        ++ side.map(x => ("side", if (x == Side.Buy) "BUY" else "SELL"))
        ++ from.map(x => ("from", x.toString))
        ++ direction.map(x => ("direction", x))
        ++ limit.map(x => ("limit", x.toString))
        )
      PrivateGet("/orders", params)
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): Seq[PoloniexOrder] =
      converter.convertOrders(json)

  }

  case class CreateOrder(
    symbol: String,
    side: Side,
    timeInForce: Option[PoloniexTimeInForce],
    `type`: Option[PoloniexOrderType], //...LIMIT_MAKER (for placing post only orders)
    accountType: Option[String],
    price: Option[BigDecimal],
    quantity: Option[BigDecimal],
    amount: Option[BigDecimal],
    clientOrderId: Option[String],
  ) extends PoloniexApiRequest[PoloniexCreateOrderResponse] {

    override def httpRequest: PoloniexHttpRequest = {
      val params = (Seq(
        ("symbol", symbol),
        ("side", if (side == Side.Sell) "SELL" else "BUY")
      )
        ++ timeInForce.map(x => ("timeInForce", x.toString))
        ++ `type`.map(x => ("type", x.toString))
        ++ accountType.map(x => ("accountType", x))
        ++ price.map(x => ("price", x.toString()))
        ++ quantity.map(x => ("quantity", x.toString()))
        ++ amount.map(x => ("amount", x.toString()))
        ++ clientOrderId.map(x => ("clientOrderId", x))
        )
      PrivatePost("/orders", params)
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): PoloniexCreateOrderResponse =
      converter.convertCreateOrderResponse(json)
  }

  case class CancelOrderById(
    orderId: String
  ) extends PoloniexApiRequest[PoloniexCancelOrderByIdResponse] {

    override def httpRequest: PoloniexHttpRequest = {
      PrivateDelete(s"/orders/$orderId", params = Seq())
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): PoloniexCancelOrderByIdResponse = {
      converter.convertCancelOrderByIdResponse(json)
    }

  }

  case class GetSymbolInfos()
    extends PoloniexApiRequest[Seq[PoloniexSymbolInfo]] {

    override def httpRequest: PoloniexHttpRequest = {
      val path = "/markets"
      PublicGet(path, Seq())
    }

    override def convertResponse(json: JsValue, converter: PoloniexJsonConverter): Seq[PoloniexSymbolInfo] = {
      converter.convertSymbolInfos(json)
    }
  }

}