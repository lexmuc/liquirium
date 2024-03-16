package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexHttpRequest._
import io.liquirium.util.{HttpResponse, HttpService, Logger}
import play.api.libs.json.{JsObject, JsString, JsValue}

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PoloniexHttpService(
  baseService: HttpService,
  authenticator: PoloniexAuthenticator,
  responseTransformer: Try[HttpResponse] => Try[JsValue],
  logger: Logger,
)(
  implicit val ec: ExecutionContext,
) {

  val BASE_URL = "https://api.poloniex.com"

  def sendRequest(request: PoloniexHttpRequest): Future[JsValue] = request match {
      case PublicGet(path, params) =>
        val url = completeUrl(path, params)
        withLoggingAndTransformation("public GET: " + url) {
          baseService.get(url, Map())
        }

      case PrivateGet(path, params) =>
        val url = completeUrl(path, params)
        val headers = authenticator.getHeadersForGetRequest(path, params)
        withLoggingAndTransformation("private GET: " + url) {
          baseService.get(url, headers)
        }

      case PrivatePost(path, params) =>
        val url = completeUrl(path, Seq())
        val body = getJsonBody(params)
        val headers = authenticator.getHeadersForPostRequest(path, body)
        withLoggingAndTransformation("private POST: " + url + " " + body) {
          baseService.postJson(url, body, headers)
        }

      case PrivateDelete(path, params) =>
        val url = completeUrl(path, Seq())
        val body = getJsonBody(params)
        val headers = authenticator.getHeadersForDeleteRequest(path, body)
        withLoggingAndTransformation("private DELETE: " + url + " " + body) {
          baseService.deleteJson(url, body, headers)
        }
    }

  private def getJsonBody(params: Seq[(String, String)]): String =
    JsObject(params.map { case (k, v) => (k, JsString(v)) }).toString

  private def withLoggingAndTransformation(requestInfo: String)(sendRequest: => Future[HttpResponse]): Future[JsValue] = {
    logger.debug(requestInfo)
    val f = sendRequest.transform(responseTransformer)
    f onComplete {
      case Success(_) => logger.debug("success")
      case Failure(e) => logger.warn("http request failed", e)
    }
    f
  }

  private def completeUrl(path: String, params: Seq[(String, String)]): String =
    s"$BASE_URL$path${if (params.nonEmpty) "?" + serialize(params) else ""}"

  private def serialize(params: Seq[(String, String)]) = {
    val encode = (s: String) => URLEncoder.encode(s, "UTF-8")
    params.map { case (k, v) => s"${encode(k)}=${encode(v)}" }.mkString("&")
  }

}
