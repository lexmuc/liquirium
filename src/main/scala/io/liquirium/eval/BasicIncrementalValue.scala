package io.liquirium.eval

import scala.annotation.tailrec

trait BasicIncrementalValue[I, T <: BasicIncrementalValue[I, T]] extends IncrementalValue[I, T] {

  def prev: Option[T]

  def lastIncrement: Option[I]

  def inc(increment: I): T

  val incCount: Int = prev match {
    case None => 0
    case Some(p) => p.incCount + 1
  }

  override def latestCommonAncestor(other: T): Option[T] = {
    @tailrec
    def go(a: T, b: T): Option[T] = {
      if (a eq b) Some(a)
      else if (a.incCount > b.incCount) go (a.prev.get, b)
      else if (b.incCount > a.incCount) go (a, b.prev.get)
      else if (a.prev.isEmpty && b.prev.isEmpty) None
      else go(a.prev.get, b.prev.get)
    }
    go(this.asInstanceOf[T], other)
  }

  override def incrementsAfter(other: T): Iterable[I] = {
    @tailrec
    def go(v: T, acc: List[I]): Iterable[I] = {
      if (v eq other) acc
      else if (v.prev.isEmpty)
        throw new RuntimeException("Value passed to incrementHistoryAfter() was not an ascendant.")
      else go(v.prev.get, v.lastIncrement.get :: acc)
    }
    go(this.asInstanceOf[T], List())
  }

  override val root: T = prev match {
    case Some(p) => p.root
    case None => this.asInstanceOf[T]
  }

  override def isAncestorOf(other: T): Boolean = {
    @tailrec
    def moveBackwards(v: T, toIncCount: Int): T =
      if (toIncCount < v.incCount) moveBackwards(v.prev.get, toIncCount) else v

    if (this.incCount > other.incCount) false else moveBackwards(other, this.incCount) eq this
  }

}
