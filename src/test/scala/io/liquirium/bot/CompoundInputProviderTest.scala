package io.liquirium.bot

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.bot.helpers.BotInputHelpers.intInput
import io.liquirium.core.helpers.TestWithMocks

class CompoundInputProviderTest extends TestWithMocks {

  private def fakeProvider[T](input: BotInput[T], output: Option[Source[T, NotUsed]]) = {
    val p = mock[BotInputProvider]
    p.getInputUpdateStream(input) returns output
    p
  }

  private def fakeProviderWithFailure[T](input: BotInput[T]) = {
    val p = mock[BotInputProvider]
    p.getInputUpdateStream(input) throws new RuntimeException("provider failed!")
    p
  }

  private def apply[T](subProviders: BotInputProvider*)(input: BotInput[T]): Option[Source[T,NotUsed]] =
    CompoundInputProvider(subProviders).getInputUpdateStream(input)

  test("it returns None for any input when it has no sub providers") {
    apply()(intInput(1)) shouldEqual None
  }

  test("it returns None when all sub provider return None") {
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), None)
    )(intInput(1)) shouldEqual None
  }

  test("it returns the first source returned by any sub-provider") {
    val sourceA = Source.repeat[Int](1)
    val sourceB = Source.repeat[Int](2)
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), Some(sourceA)),
      fakeProvider(intInput(1), Some(sourceB))
    )(intInput(1)) shouldEqual Some(sourceA)
  }

  test("it doesn't try more providers once a source is found") {
    val sourceA = Source.repeat[Int](1)
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), Some(sourceA)),
      fakeProviderWithFailure(intInput(1))
    )(intInput(1))
  }

}
