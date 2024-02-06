package io.liquirium.util.helpers

import io.liquirium.core.helpers.BasicTest
import io.liquirium.util.JsonSerializer
import play.api.libs.json.{JsValue, Json}


abstract class JsonSerializerTest[T] extends BasicTest {

  def buildSerializer() : JsonSerializer[T]

  def assertSerialization(t: T, s: String): Unit =
    Json.prettyPrint(buildSerializer().serialize(t)) shouldEqual Json.prettyPrint(Json.parse(s))

  def assertSerialization(t: T, jsValue: JsValue): Unit =
    buildSerializer().serialize(t) shouldEqual jsValue

}
