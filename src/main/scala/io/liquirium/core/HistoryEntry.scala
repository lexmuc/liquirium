package io.liquirium.core

import java.time.Instant

trait HistoryEntry extends Ordered[HistoryEntry] {

  def historyId: String

  def historyTimestamp: Instant

  override def compare(that: HistoryEntry): Int =
    if (that.historyTimestamp.isBefore(this.historyTimestamp))
      1
    else if (that.historyTimestamp == this.historyTimestamp)
      if (that.historyId < this.historyId) 1 else -1
    else
      -1

}
