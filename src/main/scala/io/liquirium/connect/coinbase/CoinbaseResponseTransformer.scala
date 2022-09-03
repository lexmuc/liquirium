package io.liquirium.connect.coinbase

import com.fasterxml.jackson.core.JsonParseException
import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.util.{Failure, Success, Try}

object CoinbaseResponseTransformer extends (Try[HttpResponse] => Try[JsValue]) {

  override def apply(t: Try[HttpResponse]): Try[JsValue] = t match {

    case Success(HttpResponse(status, s)) =>
      if (s.trim == "") Failure(CoinbaseApiError("Response was empty"))
      else {
        try {
          val json = Json.parse(s)
          status match {
            case 200 => Success(json)
            case _ =>
              if (json.isInstanceOf[JsObject] && json.as[JsObject].keys("code") && json.as[JsObject].keys("message"))
                Failure(ExplicitCoinbaseApiError(json("code").as[Int], json("message").as[String]))
              else Failure(CoinbaseApiError(s"Received response with status ${ status.toString }: $s"))
          }
        }
        catch {
          case jpe: JsonParseException => Failure(OtherCoinbaseApiError("Response is not valid json: " + s, Some(jpe)))
          case t: Throwable => Failure(CoinbaseApiError("Response has an unexpected format: " + s, Some(t)))
        }
      }

    case Failure(e) => Failure(CoinbaseApiError("The http request failed", Some(e)))

  }

}
