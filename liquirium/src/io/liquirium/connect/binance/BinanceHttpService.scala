package io.liquirium.connect.binance

import io.liquirium.util.{Clock, HttpResponse, HttpService}

import java.net.URLEncoder
import scala.concurrent.Future

class BinanceHttpService
(
  baseService: HttpService,
  authenticator: BinanceAuthenticator,
  clock: Clock,
) {

  def sendRequest(request: BinanceHttpRequest): Future[HttpResponse] = request match {

    case BinanceHttpRequest.PublicGet(endpoint, params) =>
      baseService.get(completeUrl(endpoint, params), Map())

    case BinanceHttpRequest.SignedGet(endpoint, params) =>
      baseService.get(completeUrl(endpoint, paramsWithSignature(params)), headersWithApiKey)

    case BinanceHttpRequest.SignedPost(endpoint, params) =>
      baseService.postFormData(completeUrl(endpoint, paramsWithSignature(params)), "", headersWithApiKey)

    case BinanceHttpRequest.PostWithApiKey(endpoint, params) =>
      baseService.postFormData(completeUrl(endpoint, params), "", headersWithApiKey)

    case BinanceHttpRequest.SignedDelete(endpoint, params) =>
      baseService.delete(completeUrl(endpoint, paramsWithSignature(params)), headersWithApiKey)

  }

  private def paramsWithSignature(originalParams: Seq[(String, String)]): Seq[(String, String)] = {
    val paramsWithTimestamp = originalParams :+ ("timestamp", clock.getTime.toEpochMilli.toString)
    val stringToSign = queryString(paramsWithTimestamp)
    val signature = authenticator.sign(stringToSign)
    paramsWithTimestamp :+ ("signature", signature)
  }

  private def headersWithApiKey = Map("X-MBX-APIKEY" -> authenticator.apiKey)

  private def completeUrl(path: String, params: Seq[(String, String)]): String = {
    val query = queryString(params)
    s"https://api.binance.com$path${ if (query.nonEmpty) "?" else "" }$query"
  }

  private def queryString(params: Seq[(String, String)]): String =
    params.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&")

  private def urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

}
