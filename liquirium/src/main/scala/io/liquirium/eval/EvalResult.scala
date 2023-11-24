package io.liquirium.eval

sealed trait EvalResult[+M] {

  def get: M

  def map[N](f: M => N): EvalResult[N]

}

case class InputRequest(inputs: Set[Input[_]]) extends EvalResult[Nothing] {

  if (inputs.isEmpty) throw new RuntimeException("asdf")

  def combine(other: InputRequest): InputRequest = copy(inputs.union(other.inputs))

  override def get: Nothing = throw UnknownInputsException(inputs)

  override def map[N](f: Nothing => N): EvalResult[N] = this

}

case class Value[M](v: M) extends EvalResult[M] {

  override def get: M = v

  override def map[N](f: M => N): EvalResult[N] = Value(f(v))

}
