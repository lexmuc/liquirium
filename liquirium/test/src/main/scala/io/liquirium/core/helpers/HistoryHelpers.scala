package io.liquirium.core.helpers

import io.liquirium.core.HistoryEntry
import io.liquirium.core.helpers.CoreHelpers.milli

import java.time.Instant

case object HistoryHelpers {

  case class TestHistoryEntry(historyId: String, historyTimestamp: Instant) extends HistoryEntry

  def historyEntry(id: String, time: Instant): TestHistoryEntry = TestHistoryEntry(id, time)

  def historyEntry(id: Int, time: Instant): TestHistoryEntry = TestHistoryEntry(id.toString, time)

  def historyEntry(idAndTime: Int): TestHistoryEntry = historyEntry(idAndTime, milli(idAndTime))

}
