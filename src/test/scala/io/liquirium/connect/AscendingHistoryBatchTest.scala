package io.liquirium.connect

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.milli
import io.liquirium.core.helper.HistoryHelpers.TestHistoryEntry

class AscendingHistoryBatchTest extends BasicTest {

  private def e(idAndTime: Int): TestHistoryEntry = e(idAndTime, idAndTime)

  private def e(id: Int, ts: Long = 0): TestHistoryEntry = TestHistoryEntry(id.toString, milli(ts))

  private def batch(ee: TestHistoryEntry*): AscendingHistoryBatch[TestHistoryEntry] = AscendingHistoryBatch(ee)

  test("a batch is empty if it has no entries") {
    batch(e(2)).isEmpty shouldBe false
    batch().isEmpty shouldBe true
  }

  test("the size can be accessed via a respective field") {
    batch(e(1), e(2), e(3)).size shouldBe 3
  }

  test("it can be created from unordered elements but gives access to ordered elements") {
    batch(e(4), e(5), e(2)).entries shouldEqual Seq(e(2), e(4), e(5))
    batch(e(id = 2, ts = 4), e(id = 1, ts = 4)).entries shouldEqual Seq(e(id = 1, ts = 4), e(id = 2, ts = 4))
  }

  test("reversing it turns it into a descending batch") {
    batch(e(3), e(4)).reverse shouldEqual DescendingHistoryBatch(List(e(4), e(3)))
  }

  test("inserted elements are porperly placed according to the element order") {
    batch(e(3), e(4)).insert(e(5)).entries shouldEqual Seq(e(3), e(4), e(5))
    batch(e(3), e(4)).insert(e(2)).entries shouldEqual Seq(e(2), e(3), e(4))
    batch(e(40, 4), e(42, 4)).insert(e(41, 4)).entries shouldEqual Seq(e(40, 4), e(41, 4), e(42, 4))
  }

  test("several elements can be inserted at once") {
    batch(e(3), e(6)).insertAll(Seq(e(5), e(4))).entries shouldEqual Seq(e(3), e(4), e(5), e(6))
  }

}