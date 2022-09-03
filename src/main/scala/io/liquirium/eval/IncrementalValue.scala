package io.liquirium.eval

trait IncrementalValue[I, T <: IncrementalValue[I, T]] {

  def latestCommonAncestor(other: T): Option[T]

  def incrementsAfter(other: T): Iterable[I]

  def root: T

  def isAncestorOf(other: T): Boolean

}
