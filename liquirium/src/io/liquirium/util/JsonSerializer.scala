package io.liquirium.util

import play.api.libs.json.JsValue

trait JsonSerializer[T] {

  def serialize(o: T): JsValue

}
