package io.liquirium.bot.simulation

import io.liquirium.bot.SingleMarketStrategyBot

trait SingleMarketLoggingStrategy[L <: SimulationLogger[L]] extends (SingleMarketStrategyBot => L)
