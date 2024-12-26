package io.liquirium.eval

import io.liquirium.bot.helpers.BotInputHelpers.intInput
import io.liquirium.core.helpers.{BasicTest, TestWithMocks}
import io.liquirium.util.async.Subscription
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CompositeInputSubscriptionProviderTest extends BasicTest with TestWithMocks {

  private def fakeProvider[T](input: Input[T], output: Option[Subscription[T]]) = {
    val p = mock(classOf[InputSubscriptionProvider])
    p.apply(input) returns output
    p
  }

  private def fakeProviderWithFailure[T](input: Input[T]) = {
    val p = mock(classOf[InputSubscriptionProvider])
    p.apply(input) throws new RuntimeException("provider failed!")
    p
  }

  private def apply(subProviders: InputSubscriptionProvider*)(input: Input[_]): Option[Subscription[_]] =
    new CompositeInputSubscriptionProvider(subProviders).apply(input)

  test("it returns None for any input when it has no sub providers") {
    apply()(intInput(1)) shouldEqual None
  }

  test("it returns None when all sub provider return None") {
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), None)
    )(intInput(1)) shouldEqual None
  }

  test("it returns the first subscription returned by any sub-provider") {
    val subscriptionA = mock(classOf[Subscription[Int]])
    val subscriptionB = mock(classOf[Subscription[Int]])
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), Some(subscriptionA)),
      fakeProvider(intInput(1), Some(subscriptionB)),
    )(intInput(1)) shouldEqual Some(subscriptionA)
  }

  test("it doesn't try more providers once a source is found") {
    val subscriptionA = mock(classOf[Subscription[Int]])
    apply(
      fakeProvider(intInput(1), None),
      fakeProvider(intInput(1), Some(subscriptionA)),
      fakeProviderWithFailure(intInput(1))
    )(intInput(1))
  }

}
