package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseHttpRequest.{PrivateGet, PrivatePost}
import io.liquirium.util.akka.AsyncHttpService
import io.liquirium.util.{HttpResponse, Logger}
import play.api.libs.json.JsValue

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CoinbaseHttpService(
  baseService: AsyncHttpService,
  authenticator: CoinbaseAuthenticator,
  responseTransformer: Try[HttpResponse] => Try[JsValue],
  logger: Logger,
)(
  implicit val ec: ExecutionContext,
) {

  val BASE_URL = "https://coinbase.com" // Advanced Trade API

  def sendRequest(request: CoinbaseHttpRequest): Future[JsValue] =
    request match {

      case PrivateGet(path, params) =>
        val url = completeUrl(path, params)
        val headers = authenticator.getHeadersForGetRequest(path)
        withLoggingAndTransformation("private GET: " + url) {
          baseService.get(url, headers)
        }

      case PrivatePost(path, body) =>
        val url = completeUrl(path, Seq())
        val headers = authenticator.getHeadersForPostRequest(path, body.toString())
        withLoggingAndTransformation("private POST: " + url) {
          baseService.postJson(url, body.toString(), headers)
        }

    }


  private def withLoggingAndTransformation(
    requestInfo: String,
  )(
    sendRequest: => Future[HttpResponse]
  ): Future[JsValue] = {
    logger.debug(requestInfo)
    val f = sendRequest.transform(responseTransformer)
    f onComplete {
      case Success(_) => logger.debug("success")
      case Failure(e) => logger.warn("http request failed", e)
    }
    f
  }

  private def completeUrl(path: String, params: Seq[(String, String)]): String =
    s"$BASE_URL$path${ if (params.nonEmpty) "?" + serialize(params) else "" }"

  private def serialize(params: Seq[(String, String)]) = {
    val encode = (s: String) => URLEncoder.encode(s, "UTF-8")
    params.map { case (k, v) => s"${ encode(k) }=${ encode(v) }" }.mkString("&")
  }

}
