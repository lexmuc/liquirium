package io.liquirium.util

import play.api.libs.json.{JsObject, Json}

import java.nio.file.Path

class ApiCredentialsReader(fileIO: FileIO) {

  def read(path: Path): ApiCredentials = {
    val obj = Json.parse(fileIO.read(path)).as[JsObject]
    ApiCredentials(
      apiKey = (obj \ "apiKey").as[String],
      secret = (obj \ "secret").as[String],
    )
  }

}