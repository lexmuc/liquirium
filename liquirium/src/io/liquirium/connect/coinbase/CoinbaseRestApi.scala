package io.liquirium.connect.coinbase

import io.liquirium.util.Logger
import io.liquirium.util.akka.{AsyncApi, AsyncRequest}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class CoinbaseRestApi(
  httpService: CoinbaseHttpService,
  jsonConverter: CoinbaseJsonConverter,
  logger: Logger,
)(
  implicit ec: ExecutionContext,
) extends AsyncApi[CoinbaseApiRequest[_]] {

  override def sendRequest[R](request: AsyncRequest[R] with CoinbaseApiRequest[_]): Future[R] = {
    logger.info("sending http request: " + request)
    httpService
      .sendRequest(request.httpRequest)
      .map { j => convertResponse(request, j) }
      .recover { case t =>
        logger.error(s"REST request failed: $request", t)
        throw request.convertFailure(t)
      }
  }

  private def convertResponse[R](request: AsyncRequest[R] with CoinbaseApiRequest[_], j: JsValue): R =
    try {
      val res = request.convertResponse(j, jsonConverter).asInstanceOf[R]
      logger.info("successfully received and parsed REST response")
      logger.info("response: " + res)
      res
    }
    catch {
      case e: Throwable => throw CoinbaseApiError.failedJsonConversion(j, e)
    }

}
