package io.liquirium.connect.binance

import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class BinanceExtendedHttpService(baseService: BinanceHttpService)(implicit ec: ExecutionContext) {

  def sendRequest(request: BinanceHttpRequest): Future[JsValue] =
    baseService.sendRequest(request) transform {
      case Success(HttpResponse(200, s)) =>
        try {
          Success(Json.parse(s))
        }
        catch {
          case e: Exception => Failure(BinanceApiError("Failed to parse response as json", None, Some(e)))
        }

      case Success(HttpResponse(code, s)) =>
        try {
          Failure(tryToParseAsError(s))
        }
        catch {
          case e: Exception => Failure(BinanceApiError(s"Received response with status $code: $s"))
        }

      case Failure(e) => Failure(BinanceApiError("The http request failed", None, Some(e)))
    }

  private def tryToParseAsError(s: String): BinanceApiError = {
    val keyVal = Json.parse(s).as[Map[String, JsValue]]
    val msg = keyVal("msg").as[String]
    val code = keyVal("code").as[Int]
    BinanceApiError(msg, Some(code))
  }

}
