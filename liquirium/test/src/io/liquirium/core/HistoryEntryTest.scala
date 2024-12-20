package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.HistoryHelpers.{historyEntry => he}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class HistoryEntryTest extends BasicTest {

  test("history entries are ordered primarily by time and by id as secondary criterion") {
    val s = Seq[HistoryEntry](
      he("A", sec(3)),
      he("C", sec(2)),
      he("A", sec(2)),
      he("A", sec(1)),
      he("B", sec(2)),
    )
    s.sorted shouldEqual Seq(
      he("A", sec(1)),
      he("A", sec(2)),
      he("B", sec(2)),
      he("C", sec(2)),
      he("A", sec(3)),
    )
  }

}
