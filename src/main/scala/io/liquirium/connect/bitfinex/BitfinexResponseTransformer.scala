package io.liquirium.connect.bitfinex

import com.fasterxml.jackson.core.JsonParseException
import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

object BitfinexResponseTransformer extends (Try[HttpResponse] => Try[JsValue]) {
  override def apply(t: Try[HttpResponse]): Try[JsValue] = t match {

    case Success(HttpResponse(status, s)) =>
      if (s.trim == "") Failure(BitfinexApiError("Response was empty"))
      else {
        try {
          val json = Json.parse(s)
          status match {
            case 200 => Success(json)
            case _ => Failure(ExplicitBitfinexApiError(s"Received response with status ${ status.toString }: $s"))
          }
        }
        catch {
          case jpe: JsonParseException => Failure(OtherBitfinexApiError("Response is not valid json: " + s, Some(jpe)))
          case t: Throwable => Failure(BitfinexApiError("Response has an unexpected format: " + s, Some(t)))
        }
      }

    case Failure(e) => Failure(BitfinexApiError("The http request failed", Some(e)))

  }

}
