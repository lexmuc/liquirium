package io.liquirium.eval

import scala.collection.SeqLike


object IncrementalSeq {

  def empty[T]: IncrementalSeq[T] = Impl(Vector[T](), None)

  def from[T](iterable: Iterable[T]): IncrementalSeq[T] = iterable.foldLeft(empty[T])(_.inc(_))

  def apply[T](elements: T*): IncrementalSeq[T] = from(elements)

  private case class Impl[T](baseSeq: Vector[T], prev: Option[IncrementalSeq[T]]) extends IncrementalSeq[T] {

    override def length: Int = baseSeq.length

    override def iterator: Iterator[T] = baseSeq.iterator

    override def lastIncrement: Option[T] = baseSeq.lastOption

    override def lastOption: Option[T] = lastIncrement

    override def reverseIterator: Iterator[T] = baseSeq.reverseIterator

    override def inc(increment: T): IncrementalSeq[T] = Impl[T](baseSeq :+ increment, Some(this))

    override def apply(idx: Int): T = baseSeq(idx)

    override def equals(that: Any): Boolean = that match {
      case that: IncrementalSeq[_] =>
        (that eq this.asInstanceOf[IncrementalSeq[_]]) || (that.reverseIterator sameElements this.reverseIterator)
      case _ => super.equals(that)
    }

  }

}

trait IncrementalSeq[T] extends BasicIncrementalValue[T, IncrementalSeq[T]] with Seq[T] with SeqLike[T, Seq[T]]
