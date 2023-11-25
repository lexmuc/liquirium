package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.PublicGet
import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.core.Side
import io.liquirium.util.Logger
import io.liquirium.util.akka.{AsyncApi, AsyncRequest}
import play.api.libs.json.JsValue

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

object BinanceRestApi {

  sealed trait BinanceApiRequest[T] extends AsyncRequest[T] {
    def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest

    def convertResponse(json: JsValue, converter: BinanceJsonConverter): T
  }

  case class CandlesRequest(
    symbol: String,
    interval: BinanceCandleLength,
    limit: Option[Int],
    from: Option[Instant],
    until: Option[Instant]
  ) extends BinanceApiRequest[Seq[BinanceCandle]] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest = {
      val params = mandatoryParams ++ limit.map(l => ("limit", l.toString)).toSeq ++ timeParams
      BinanceHttpRequest.PublicGet("/api/v3/klines", params)
    }

    private def mandatoryParams = Seq(("symbol", symbol), ("interval", interval.code))

    private def timeParams =
      from.map(f => ("startTime", f.toEpochMilli.toString)) ++
        until.map(u => ("endTime", (u.toEpochMilli - 1).toString))

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter) : Seq[BinanceCandle] =
      converter.convertCandles(json)
  }

  case class OpenOrdersRequest(symbol: Option[String]) extends BinanceApiRequest[Seq[BinanceOrder]] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest = {
      val params = symbol.map(s => ("symbol", s)).toSeq
      BinanceHttpRequest.SignedGet("/api/v3/openOrders", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): Seq[BinanceOrder] = {
      converter.convertOrders(json)
    }

  }

  case class GetTradesRequest(
    symbol: String,
    startTime: Option[Instant],
    endTime: Option[Instant],
    limit: Option[Int],
  )
    extends BinanceApiRequest[Seq[BinanceTrade]] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest = {
      val params = (Seq(("symbol", symbol))
        ++ startTime.map(x => ("startTime", x.toEpochMilli.toString))
        ++ endTime.map(x => ("endTime", x.toEpochMilli.toString))
        ++ limit.map(x => ("limit", x.toString))
        )
      BinanceHttpRequest.SignedGet("/api/v3/myTrades", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): Seq[BinanceTrade] =
      converter.convertTrades(json)

  }

  case class NewOrderRequest(
    side: Side,
    symbol: String,
    quantity: BigDecimal,
    price: BigDecimal,
    orderType: BinanceOrderType,
  ) extends BinanceApiRequest[BinanceOrder] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest = {
      val optTimeInForce = if (orderType == BinanceOrderType.LIMIT) Some(("timeInForce", "GTC")) else None
      val params = Seq(
        ("side", if (side == Side.Buy) "BUY" else "SELL"),
        ("symbol", symbol),
        ("quantity", quantity.bigDecimal.toPlainString),
        ("price", price.bigDecimal.toPlainString),
        ("type", converter.convertOrderType(orderType)),
        ("newOrderRespType", "RESULT"),
      ) ++ optTimeInForce

      BinanceHttpRequest.SignedPost("/api/v3/order", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): BinanceOrder =
      converter.convertSingleOrder(json)

  }

  case class CancelOrderRequest(symbol: String, orderId: String) extends BinanceApiRequest[BinanceOrder] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest =
      BinanceHttpRequest.SignedDelete("/api/v3/order", Seq(
        ("symbol", symbol),
        ("orderId", orderId),
      ))

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): BinanceOrder =
      converter.convertSingleOrder(json)

  }

  case class CreateListenKey(margin: Boolean) extends BinanceApiRequest[String] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest =
      if (margin) BinanceHttpRequest.PostWithApiKey("/sapi/v3/userDataStream", Seq())
      else BinanceHttpRequest.PostWithApiKey("/api/v3/userDataStream", Seq())

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): String =
      json("listenKey").as[String]

  }

  case class GetExchangeInfo() extends BinanceApiRequest[Seq[BinanceSymbolInfo]] {

    override def httpRequest(converter: BinanceJsonConverter): BinanceHttpRequest = {
      val path = "/api/v3/exchangeInfo"
      PublicGet(path, Seq())
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): Seq[BinanceSymbolInfo] = {
      val symbols = json("symbols")
      converter.convertSymbolInfos(symbols)
    }
  }

}

class BinanceRestApi(
  httpService: BinanceExtendedHttpService,
  jsonConverter: BinanceJsonConverter,
  logger: Logger,
)(implicit ec: ExecutionContext) extends AsyncApi[BinanceApiRequest[_]] {

  override def sendRequest[R](request: AsyncRequest[R] with BinanceApiRequest[_]): Future[R] = {
    logger.info(s"sending REST request: $request")
    httpService
      .sendRequest(request.httpRequest(jsonConverter))
      .map { j =>
        try {
          val res = request.convertResponse(j, jsonConverter).asInstanceOf[R]

          logger.info("successfully received and parsed REST response: " + (res.toString).take(200))
          res
        }
        catch {
          case e: Throwable => throw BinanceApiError.failedJsonConversion(j, e)
        }
      }
      .recover { case t =>
        logger.warn(s"REST request failed: $request", t)
        throw t
      }
  }

}

