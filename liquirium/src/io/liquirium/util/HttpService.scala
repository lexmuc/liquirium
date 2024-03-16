package io.liquirium.util

import scala.concurrent.Future


trait HttpService {

  def postFormData(url: String, data: String, headers: Map[String, String] = Map()): Future[HttpResponse]

  def postJson(url: String, data: String, headers: Map[String, String] = Map()): Future[HttpResponse]

  def get(url: String, headers: Map[String, String]): Future[HttpResponse]

  def delete(url: String, headers: Map[String, String]): Future[HttpResponse]

  def deleteJson(url: String, data: String, headers: Map[String, String]): Future[HttpResponse]

}
