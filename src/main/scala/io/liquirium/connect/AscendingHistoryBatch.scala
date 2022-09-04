package io.liquirium.connect

import io.liquirium.core.HistoryEntry

trait AscendingHistoryBatch[E <: HistoryEntry] {

  def size: Int

  def entries: List[E]

  def isEmpty: Boolean

  def reverse: DescendingHistoryBatch[E]

  def insert(entry: E): AscendingHistoryBatch[E]

  def insertAll(entries: Iterable[E]): AscendingHistoryBatch[E]

}

object AscendingHistoryBatch {

  def apply[E <: HistoryEntry](entries: Iterable[E]): AscendingHistoryBatch[E] =
    Impl(entries.toList.sorted(Ordering[HistoryEntry]))

  private case class Impl[E <: HistoryEntry](entries: List[E]) extends AscendingHistoryBatch[E] {

    override val size: Int = entries.size

    override val isEmpty: Boolean = size == 0

    override def reverse: DescendingHistoryBatch[E] = DescendingHistoryBatch(entries.reverse)

    override def insert(entry: E): AscendingHistoryBatch[E] = {
      val greaterEntries = entries.dropWhile(_.compare(entry) < 0)
      val smallerEntries = entries.takeWhile(_.compare(entry) < 0)
      copy(smallerEntries ++ (entry :: greaterEntries))
    }

    override def insertAll(entries: Iterable[E]): AscendingHistoryBatch[E] =
      entries.foldLeft(this.asInstanceOf[AscendingHistoryBatch[E]]) { _.insert(_) }

  }

}