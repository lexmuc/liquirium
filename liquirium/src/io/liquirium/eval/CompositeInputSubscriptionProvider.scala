package io.liquirium.eval
import io.liquirium.util.async.Subscription

class CompositeInputSubscriptionProvider(
  providers: Seq[InputSubscriptionProvider]
) extends InputSubscriptionProvider {

  override def apply(input: Input[_]): Option[Subscription[_]] =
    providers.view.flatMap(_.apply(input)).headOption

}
