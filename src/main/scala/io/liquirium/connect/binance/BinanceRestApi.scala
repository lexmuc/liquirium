package io.liquirium.connect.binance

import io.liquirium.connect.AscendingHistoryBatch
import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.core.Side
import io.liquirium.util.Logger
import io.liquirium.util.akka.{AsyncApi, AsyncRequest}
import play.api.libs.json.JsValue

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

object BinanceRestApi {

  sealed trait BinanceApiRequest[T] extends AsyncRequest[T] {
    def httpRequest: BinanceHttpRequest

    def convertResponse(json: JsValue, converter: BinanceJsonConverter): T
  }

  case class CandlesRequest(
    symbol: String,
    resolution: BinanceCandleResolution,
    limit: Option[Int],
    from: Option[Instant],
    until: Option[Instant]
  ) extends BinanceApiRequest[AscendingHistoryBatch[BinanceCandle]] {

    override def httpRequest: BinanceHttpRequest = {
      val params = mandatoryParams ++ limit.map(l => ("limit", l.toString)).toSeq ++ timeParams
      BinanceHttpRequest.PublicGet("/api/v3/klines", params)
    }

    private def mandatoryParams = Seq(("symbol", symbol), ("interval", resolution.code))

    private def timeParams =
      from.map(f => ("startTime", f.toEpochMilli.toString)) ++
        until.map(u => ("endTime", (u.toEpochMilli - 1).toString))

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter)
    : AscendingHistoryBatch[BinanceCandle] = {
      val cc = converter.convertCandles(json)
      AscendingHistoryBatch(cc)
    }
  }

  case class OpenOrdersRequest(symbol: Option[String]) extends BinanceApiRequest[Set[BinanceOrder]] {

    override def httpRequest: BinanceHttpRequest = {
      val params = symbol.map(s => ("symbol", s)).toSeq
      BinanceHttpRequest.SignedGet("/api/v3/openOrders", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): Set[BinanceOrder] = {
      converter.convertOrders(json).toSet
    }

  }

  case class GetTradesRequest(symbol: String, startTime: Option[Long], endTime: Option[Long], limit: Option[Int])
    extends BinanceApiRequest[Seq[BinanceTrade]] {

    override def httpRequest: BinanceHttpRequest = {
      val params = (Seq(("symbol", symbol))
        ++ startTime.map(x => ("startTime", x.toString))
        ++ endTime.map(x => ("endTime", x.toString))
        ++ limit.map(x => ("limit", x.toString))
        )
      BinanceHttpRequest.SignedGet("/api/v3/myTrades", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): Seq[BinanceTrade] =
      converter.convertTrades(json)

  }

  case class NewOrderRequest
  (
    side: Side,
    symbol: String,
    quantity: BigDecimal,
    price: BigDecimal,
    orderType: String
  ) extends BinanceApiRequest[BinanceOrder] {

    override def httpRequest: BinanceHttpRequest = {
      val optTimeInForce = if (orderType == "LIMIT") Some(("timeInForce", "GTC")) else None
      val params = Seq(
        ("side", if (side == Side.Buy) "BUY" else "SELL"),
        ("symbol", symbol),
        ("quantity", quantity.bigDecimal.toPlainString),
        ("price", price.bigDecimal.toPlainString),
        ("type", orderType),
        ("newOrderRespType", "RESULT"),
      ) ++ optTimeInForce

      BinanceHttpRequest.SignedPost("/api/v3/order", params)
    }

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): BinanceOrder =
      converter.convertSingleOrder(json)

  }

  case class CancelOrderRequest(symbol: String, orderId: String) extends BinanceApiRequest[BinanceOrder] {

    override def httpRequest: BinanceHttpRequest =
      BinanceHttpRequest.SignedDelete("/api/v3/order", Seq(
        ("symbol", symbol),
        ("orderId", orderId),
      ))

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): BinanceOrder =
      converter.convertSingleOrder(json)

  }

  case class CreateListenKey(margin: Boolean) extends BinanceApiRequest[String] {

    override def httpRequest: BinanceHttpRequest =
      if (margin) BinanceHttpRequest.PostWithApiKey("/sapi/v3/userDataStream", Seq())
      else BinanceHttpRequest.PostWithApiKey("/api/v3/userDataStream", Seq())

    override def convertResponse(json: JsValue, converter: BinanceJsonConverter): String =
      json("listenKey").as[String]

  }

}

class BinanceRestApi(
  httpService: BinanceExtendedHttpService,
  jsonConverter: BinanceJsonConverter,
  logger: Logger
)(implicit ec: ExecutionContext) extends AsyncApi[BinanceApiRequest[_]] {

  override def sendRequest[R](request: AsyncRequest[R] with BinanceApiRequest[_]): Future[R] = {
    logger.info(s"sending REST request: $request")
    httpService
      .sendRequest(request.httpRequest)
      .map { j =>
        try {
          val res = request.convertResponse(j, jsonConverter).asInstanceOf[R]
          logger.info("successfully received and parsed REST response")
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

