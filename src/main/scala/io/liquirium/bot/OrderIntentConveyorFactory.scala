package io.liquirium.bot

import io.liquirium.core.{Market, OrderConstraints}
import io.liquirium.eval.Eval

import java.time.Instant

trait OrderIntentConveyorFactory extends ((Market, OrderConstraints, Instant) => Eval[OrderIntentConveyor])
