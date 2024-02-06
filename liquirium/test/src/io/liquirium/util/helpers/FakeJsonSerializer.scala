package io.liquirium.util.helpers

import io.liquirium.util.JsonSerializer
import org.scalatest.Assertions
import play.api.libs.json.{JsValue, Json}

case class FakeJsonSerializer[T](mappings: (T, JsValue)*) extends JsonSerializer[T] with Assertions {

  private val map = mappings.toMap

  override def serialize(o: T): JsValue =
    if (map.contains(o)) map.apply(o)
    else fail("Fake serializer received unexpected object: " + o)

}

object FakeJsonSerializer {

//  def apply[T](mappings: (T, String)*): FakeJsonSerializer[T] =
//    apply(mappings.map { case (obj, s) => obj -> Json.parse(s) }: _*)

}
