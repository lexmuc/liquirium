package io.liquirium.eval

case class UnknownInputsException(inputs: Set[Input[_]]) extends Exception {

  override def getMessage: String = s"Unknown inputs: $inputs"

}
