package io.liquirium.util

import cats.effect.IO
import org.http4s._
import org.http4s.client._
import io.liquirium.util.{HttpResponse => LiquiriumHttpResponse, HttpService => LiquiriumHttpService}
import org.typelevel.ci.CIString
import cats.effect.unsafe.IORuntime
import scala.concurrent.Future


class EmberHttpService(client: Client[IO])(implicit runtime: IORuntime) extends LiquiriumHttpService {

  private def convertHeaders(headers: Map[String, String]): Headers =
    Headers(headers.map { case (k, v) => Header.Raw(CIString(k), v) }.toList)

  private def getRequest(url: String, headers: Map[String, String]): Request[IO] =
    Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(url),
      headers = convertHeaders(headers)
    )

  private def deleteRequest(url: String, headers: Map[String, String]): Request[IO] =
    Request[IO](
      method = Method.DELETE,
      uri = Uri.unsafeFromString(url),
      headers = convertHeaders(headers)
    )

  private def deleteRequestWithBody(
    url: String,
    data: String,
    headers: Map[String, String],
    contentType: MediaType
  ): Request[IO] =
    Request[IO](
      method = Method.DELETE,
      uri = Uri.unsafeFromString(url),
      headers = convertHeaders(headers)
    ).withEntity(data)(EntityEncoder.stringEncoder(Charset.`UTF-8`))

  private def postRequest(
    url: String,
    data: String,
    headers: Map[String, String],
    contentType: MediaType
  ): Request[IO] =
    Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(url),
      headers = convertHeaders(headers)
    ).withEntity(data)(EntityEncoder.stringEncoder(Charset.`UTF-8`))

  private def process(req: Request[IO]): Future[LiquiriumHttpResponse] =
    client.run(req).use { response =>
      response.as[String].map { body =>
        LiquiriumHttpResponse(response.status.code, body)
      }
    }.unsafeToFuture()

  override def postFormData(
    url: String,
    data: String,
    headers: Map[String, String] = Map()
  ): Future[LiquiriumHttpResponse] =
    process(postRequest(url, data, headers, MediaType.application.`x-www-form-urlencoded`))

  override def postJson(
    url: String,
    data: String,
    headers: Map[String, String] = Map()
  ): Future[LiquiriumHttpResponse] =
    process(postRequest(url, data, headers, MediaType.application.json))

  override def get(url: String, headers: Map[String, String]): Future[LiquiriumHttpResponse] =
    process(getRequest(url, headers))

  override def delete(url: String, headers: Map[String, String]): Future[LiquiriumHttpResponse] =
    process(deleteRequest(url, headers))

  override def deleteJson(
    url: String,
    data: String,
    headers: Map[String, String]
  ): Future[LiquiriumHttpResponse] =
    process(deleteRequestWithBody(url, data, headers, MediaType.application.json))

}
