package io.liquirium.connect

import io.liquirium.core.HistoryEntry

trait DescendingHistoryBatch[E <: HistoryEntry] {

  def size: Int

  def entries: List[E]

  def isEmpty: Boolean

  def reverse: AscendingHistoryBatch[E]

  def insert(entry: E): DescendingHistoryBatch[E]

  def insertAll(entries: Iterable[E]): DescendingHistoryBatch[E]

}

object DescendingHistoryBatch {

  def apply[E <: HistoryEntry](entries: Iterable[E]): DescendingHistoryBatch[E] =
    Impl(entries.toList.sorted(Ordering[HistoryEntry]).reverse)

  private case class Impl[E <: HistoryEntry](entries: List[E]) extends DescendingHistoryBatch[E] {

    override val size: Int = entries.size

    override val isEmpty: Boolean = size == 0

    override def reverse: AscendingHistoryBatch[E] = AscendingHistoryBatch(entries.reverse)

    override def insert(entry: E): DescendingHistoryBatch[E] = {
      val smallerEntries = entries.dropWhile(_.compare(entry) > 0)
      val greaterEntries = entries.takeWhile(_.compare(entry) > 0)
      copy(greaterEntries ++ (entry :: smallerEntries))
    }

    override def insertAll(entries: Iterable[E]): DescendingHistoryBatch[E] =
      entries.foldLeft(this.asInstanceOf[DescendingHistoryBatch[E]]) { _.insert(_) }

  }

}
