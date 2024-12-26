package io.liquirium.eval

import io.liquirium.util.async.Subscription

trait InputSubscriptionProvider extends (Input[_] => Option[Subscription[_]])
