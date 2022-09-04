package io.liquirium.helper

import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue}

object JsonTestHelper {

  def emptyArray: JsArray = JsArray(Seq())

  def emptyObject: JsObject = JsObject(Seq())

  def json(n: Int): JsValue = JsArray(Seq(JsNumber(n)))

  def obj(n: Int): JsObject = JsObject(Seq(n.toString -> JsNumber(n)))

}
