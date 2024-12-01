package io.liquirium.connect.deribit.helpers

import io.liquirium.connect.deribit.DeribitInstrument
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

object DeribitInstrumentHelpers {

  def futureInstrument(n: Int): DeribitInstrument = {
    val i = DeribitInstrument(s"BTC-FUTURE$n")
    i.isFuture shouldBe true
    i
  }

  def optionInstrument(n: Int): DeribitInstrument = {
    val i = DeribitInstrument(s"BTC-OPTION-$n-P")
    i.isOption shouldBe true
    i
  }

}
