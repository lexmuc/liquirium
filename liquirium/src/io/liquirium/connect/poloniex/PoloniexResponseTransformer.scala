package io.liquirium.connect.poloniex

import com.fasterxml.jackson.core.JsonParseException
import io.liquirium.util.HttpResponse
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.util.{Failure, Success, Try}

object PoloniexResponseTransformer extends (Try[HttpResponse] => Try[JsValue]) {
  override def apply(t: Try[HttpResponse]): Try[JsValue] = t match {

    case Success(HttpResponse(status, s)) =>
      if (s.trim == "") Failure(PoloniexApiError("Response was empty - status " + status))
      else {
        try {
          val json = Json.parse(s)
          status match {
            case 200 => Success(json)
            case _ =>
              if (json.isInstanceOf[JsObject] && json.as[JsObject].keys("code") && json.as[JsObject].keys("message"))
                Failure(ExplicitPoloniexApiError(json("code").as[Int], json("message").as[String]))
              else Failure(PoloniexApiError(s"Received response with status ${ status.toString }: $s"))
          }
        }
        catch {
          case jpe: JsonParseException =>
            if (s.contains("CloudFlare") || s.contains("::CLOUDFLARE_ERROR") || s.contains("cloudflare-nginx"))
              Failure(PoloniexApiError("Response could not be parsed. Looks like a CloudFlare error.", Some(jpe)))
            else if (status == 502 && s.contains("CLOUDFLARE_ERROR"))
              Failure(PoloniexApiError("Received status code 502. Looks like a CloudFlare error."))
            else
              Failure(OtherPoloniexApiError("Response is not valid json: " + s, Some(jpe)))
          case t: Throwable => Failure(PoloniexApiError("Response has an unexpected format: " + s, Some(t)))
        }
      }

    case Failure(e) => Failure(PoloniexApiError("The http request failed", Some(e)))

  }

}
