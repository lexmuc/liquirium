package io.liquirium.util.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.liquirium.util.{HttpResponse => LiquiriumHttpResponse, HttpService => LiquiriumHttpService}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AkkaHttpService(implicit val actorSystem: ActorSystem) extends LiquiriumHttpService {

  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  override def postFormData(
    url: String,
    data: String,
    headers: Map[String, String] = Map(),
  ): Future[LiquiriumHttpResponse] =
    process(postRequest(url, data, headers, ContentTypes.`application/x-www-form-urlencoded`))

  override def postJson(
    url: String,
    data: String,
    headers: Map[String, String] = Map(),
  ): Future[LiquiriumHttpResponse] =
    process(postRequest(url, data, headers, ContentTypes.`application/json`))

  override def get(url: String, headers: Map[String, String]): Future[LiquiriumHttpResponse] =
    process(getRequest(url, headers))

  override def delete(url: String, headers: Map[String, String]): Future[LiquiriumHttpResponse] =
    process(deleteRequest(url, headers))

  override def deleteJson(url: String, data: String, headers: Map[String, String]): Future[LiquiriumHttpResponse] =
    process(deleteRequestWithBody(url, data, headers, ContentTypes.`application/json`))

  private def convertHeaders(hh: Map[String, String]): scala.collection.immutable.Seq[HttpHeader] =
    hh.map { case (k, v) => RawHeader(k, v) }.toList

  private def getRequest(url: String, headers: Map[String, String]) = HttpRequest(
    method = HttpMethods.GET,
    uri = url,
    headers = convertHeaders(headers)
  )

  private def deleteRequest(url: String, headers: Map[String, String]) = HttpRequest(
    method = HttpMethods.DELETE,
    uri = url,
    headers = convertHeaders(headers)
  )

  private def deleteRequestWithBody(
    url: String,
    data: String,
    headers: Map[String, String],
    contentType: ContentType,
  ) = HttpRequest(
    method = HttpMethods.DELETE,
    uri = url,
    headers = convertHeaders(headers),
    entity = HttpEntity(contentType, data.getBytes("UTF-8")),
  )

  private def postRequest(url: String, data: String, headers: Map[String, String], contentType: ContentType) =
    HttpRequest(
      method = HttpMethods.POST,
      uri = url,
      headers = convertHeaders(headers),
      entity = HttpEntity(contentType, data.getBytes("UTF-8"))
    )

  private def process(req: HttpRequest) = Http().singleRequest(req).flatMap { res =>
    Unmarshal(res.entity).to[String].map(body => LiquiriumHttpResponse(res.status.intValue(), body))
  }

}
