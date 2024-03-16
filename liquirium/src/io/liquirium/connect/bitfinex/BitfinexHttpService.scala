package io.liquirium.connect.bitfinex

import io.liquirium.util.{HttpResponse, HttpService, Logger, NonceGenerator}
import play.api.libs.json.JsValue

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BitfinexHttpService(
  baseService: HttpService,
  authenticator: BitfinexAuthenticator,
  nonceGenerator: NonceGenerator,
  responseTransformer: Try[HttpResponse] => Try[JsValue],
  logger: Logger,
)(
  implicit val ec: ExecutionContext,
) {

  def sendRequest(request: BitfinexHttpRequest): Future[JsValue] = request match {
    case PublicBitfinexGetRequest(lastPathSegment, params) =>
      val url = completeUrl(lastPathSegment, params)
      withLoggingAndTransformation("public GET: " + url) {
        baseService.get(url, Map())
      }

    case PrivateBitfinexPostRequest(lastPathSegment, body, params) =>
      val url = completeUrl(lastPathSegment, params)
      withLoggingAndTransformation("private POST: " + url) {
        baseService.postFormData(url, body, headers(lastPathSegment, body))
      }
  }

  private def getSignature(path: String, nonce: Long, body: String): String =
    authenticator.sign("/api/v2/" + path + nonce.toString + body)

  private def completeUrl(path: String, params: Seq[(String, String)]): String = {
    val query = params.map { case (k, v) => encode(k) + "=" + encode(v) }.mkString("&")
    s"https://api.bitfinex.com/v2/$path${ if (query.nonEmpty) "?" else "" }$query"
  }

  private def encode(s: String) = URLEncoder.encode(s, "UTF-8")

  private def headers(path: String, body: String) = {
    val nonce = nonceGenerator.next()
    Map(
      "Accept" -> "application/json",
      "bfx-nonce" -> nonce.toString,
      "bfx-apikey" -> authenticator.apiKey,
      "bfx-signature" -> getSignature(path, nonce, body)
    )
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

}
