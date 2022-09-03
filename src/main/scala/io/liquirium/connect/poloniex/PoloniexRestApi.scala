package io.liquirium.connect.poloniex

import io.liquirium.util.Logger
import io.liquirium.util.akka.{AsyncApi, AsyncRequest}

import scala.concurrent.{ExecutionContext, Future}

class PoloniexRestApi(
  httpService: PoloniexHttpService,
  jsonConverter: PoloniexJsonConverter,
  logger: Logger,
)(
  implicit ec: ExecutionContext,
) extends AsyncApi[PoloniexApiRequest[_]] {

  override def sendRequest[R](request: AsyncRequest[R] with PoloniexApiRequest[_]): Future[R] = {
    logger.info("sending http request: " + request)
    httpService
      .sendRequest(request.httpRequest)
      .map { j =>
        try {
          val res = request.convertResponse(j, jsonConverter).asInstanceOf[R]
          logger.info("successfully received and parsed REST response")
          logger.info("response: " + res)
          res
        }
        catch {
          case e: Throwable => throw PoloniexApiError.failedJsonConversion(j, e)
        }
      }
      .recover { case t =>
        logger.error(s"REST request failed: $request", t)
        throw request.convertFailure(t)
      }
  }

}
