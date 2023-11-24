package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.BitfinexApiRequest
import io.liquirium.util.akka.{AsyncApi, AsyncRequest}
import io.liquirium.util.{Logger, ResultOrder}
import play.api.libs.json.JsValue

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

object BitfinexRestApi {

  sealed trait BitfinexApiRequest[T] extends AsyncRequest[T] {
    def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest

    def convertResponse(json: JsValue, converter: BitfinexJsonConverter): T
  }

  case class GetTradeHistory(
    symbol: Option[String],
    from: Option[Instant],
    until: Option[Instant],
    limit: Int,
    sort: ResultOrder = ResultOrder.DescendingOrder,
  ) extends BitfinexApiRequest[Seq[BitfinexTrade]] {

    private def path = s"auth/r/trades/${ if (symbol.isDefined) symbol.get + "/" else "" }hist"

    private def timeParams =
      from.map(f => ("start", f.toEpochMilli.toString)) ++ until.map(u => ("end", (u.toEpochMilli - 1).toString))

    private val sortParam = sort match {
      case ResultOrder.DescendingOrder => "-1"
      case ResultOrder.AscendingOrder => "1"
    }

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      PrivateBitfinexPostRequest(path, "", Seq(("limit", limit.toString), ("sort", sortParam)) ++ timeParams)
    }

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): Seq[BitfinexTrade] =
      converter.convertTrades(json)

  }

  case class GetCandles(
    symbol: String,
    candleLength: BitfinexCandleLength,
    limit: Option[Int],
    from: Option[Instant],
    until: Option[Instant],
    sort: ResultOrder = ResultOrder.DescendingOrder,
  ) extends BitfinexApiRequest[Seq[BitfinexCandle]] {

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      val sortParams = Seq(("sort", if (sort == ResultOrder.AscendingOrder) "1" else "-1"))
      val params = limit.map(l => ("limit", l.toString)).toSeq ++ timeParams ++ sortParams
      PublicBitfinexGetRequest(s"candles/trade:${ candleLength.code }:$symbol/hist", params)
    }

    private def timeParams =
      from.map(f => ("start", f.toEpochMilli.toString)) ++ until.map(u => ("end", (u.toEpochMilli - 1).toString))

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): Seq[BitfinexCandle] =
      converter.convertCandles(json)

  }

  case class GetOrderHistory(
    symbol: Option[String],
    from: Option[Instant],
    until: Option[Instant],
    limit: Int,
  ) extends BitfinexApiRequest[Seq[BitfinexOrder]] {

    private def path = s"auth/r/orders/${ if (symbol.isDefined) symbol.get + "/" else "" }hist"

    private def timeParams =
      from.map(f => ("start", f.toEpochMilli.toString)) ++ until.map(u => ("end", (u.toEpochMilli - 1).toString))

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest =
      PrivateBitfinexPostRequest(path, "", Seq(("limit", limit.toString)) ++ timeParams)

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): Seq[BitfinexOrder] =
      converter.convertOrders(json)

  }

  case class GetOpenOrders(
    symbol: Option[String],
  ) extends BitfinexApiRequest[Seq[BitfinexOrder]] {

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      val path = s"auth/r/orders/${ if (symbol.isDefined) symbol.get else "" }"
      PrivateBitfinexPostRequest(path, "", Seq())
    }

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): Seq[BitfinexOrder] =
      converter.convertOrders(json)
  }

  case class SubmitOrder(
    `type`: BitfinexOrder.OrderType,
    symbol: String,
    price: Option[BigDecimal],
    amount: BigDecimal,
    flags: Set[BitfinexOrderFlag]
  ) extends BitfinexApiRequest[BitfinexOrder] {

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      val path = "auth/w/order/submit"
      val t = converter.convertOrderType(`type`).as[String]
      val flagValues: Map[BitfinexOrderFlag, Int] = Map(
        BitfinexOrderFlag.Hidden -> 64,
        BitfinexOrderFlag.Close -> 512,
        BitfinexOrderFlag.ReduceOnly -> 1024,
        BitfinexOrderFlag.PostOnly -> 4096,
        BitfinexOrderFlag.OCO -> 16384,
        BitfinexOrderFlag.NoVarRates -> 524288,
      )
      //Todo flag in Json Body Int statt String
      val flagSum = flags.map(flag => flagValues(flag)).sum.toString
      val flagParam = if (flagSum == "0") Seq() else Seq(("flags", flagSum))

      val params = Seq(("symbol", symbol), ("amount", amount.toString()), ("type", t)) ++
        price.map(p => ("price", p.toString())) ++ flagParam

      PrivateBitfinexPostRequest(path, "", params)
    }

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): BitfinexOrder = {
      val orders = (json \ 4).as[Seq[JsValue]]
      val order = orders(0)
      converter.convertSingleOrder(order)
    }

  }

  case class CancelOrder(
    id: Int,
  ) extends BitfinexApiRequest[BitfinexOrder] {
    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      val lastPathSegment = "auth/w/order/cancel"
      val params = Seq(("id", id.toString))
      PrivateBitfinexPostRequest(lastPathSegment, "", params)
    }

    override def convertResponse(json: JsValue, converter: BitfinexJsonConverter): BitfinexOrder = {
      val order = (json \ 4).as[JsValue]
      converter.convertSingleOrder(order)
    }
  }

  case class GetPairInfos() extends BitfinexApiRequest[Seq[BitfinexPairInfo]] {

    override def httpRequest(converter: BitfinexJsonConverter): BitfinexHttpRequest = {
      val path = "conf/pub:info:pair"
      PublicBitfinexGetRequest(path, Seq())
    }

    override def convertResponse(v: JsValue, converter: BitfinexJsonConverter): Seq[BitfinexPairInfo] =
      converter.convertPairInfos(v(0).as[JsValue])
  }

}

class BitfinexRestApi(
  httpService: BitfinexHttpService,
  jsonConverter: BitfinexJsonConverter,
  logger: Logger,
)(
  implicit ec: ExecutionContext,
) extends AsyncApi[BitfinexApiRequest[_]] {

  override def sendRequest[R](request: AsyncRequest[R] with BitfinexApiRequest[_]): Future[R] = {
    logger.info(s"sending REST request: $request")
    httpService
      .sendRequest(request.httpRequest(jsonConverter))
      .map { j =>
        try {
          val res = request.convertResponse(j, jsonConverter).asInstanceOf[R]
          logger.info("successfully received and parsed REST response")
          res
        }
        catch {
          case e: Throwable => throw BitfinexApiError.failedJsonConversion(j, e)
        }
      }
      .recover { case t =>
        logger.error(s"REST request failed: $request", t)
        throw t
      }
  }

}