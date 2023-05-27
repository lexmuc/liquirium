# Liquirium
Functional framework for automated trading.

## Description

Liquirium is a functional framework for automated cryptocurrency trading written in Scala.
It aims to facilitate the development of complex trading
bots based on a minimal set of simple abstractions for market activity and trading operations.
Interaction with the exchanges can be described at an abstract level independent of individual HTTP requests or
websocket messages.

The software consists of the following major packages:

- **liquirium.core:** Core data model. Common classes for orders, trades, candles, etc.

- **liquirium.connect:** Connectors to several crypto exchanges (Binance, Bitfinex, Poloniex, ...).

- **liquirium.bot:** A simple framework for developing and running functional automated trading-bots. It
  comprises utilities for simulation and backtesting.

- **liquirium.eval:** An experimental library for the incremental evaluation of complex expressions
  over market data and trading history. It makes it possible to define functional, truly stateless
  trading bots that act only based on market history and recent interactions with the exchanges.

The name Liquirium is derived from the term *liquidity* since the software was initially designed to be used for
the development of trading bots that provide market liquidity, such as automated market makers.
However, today liquirium aspires to facilitate more general trading software solutions.


## Disclaimer

The software is experimental in nature and may not be suitable for production environments. It may contain bugs,
errors, or unforeseen issues that could lead to system malfunctions, or financial losses.
The project contributors, maintainers, and associated parties make no warranties or guarantees
regarding the software's functionality, performance, or suitability for any specific purpose.
The software is provided without any express or implied warranties. Use it at your own risk.

## State of development

liquirium.bot and liquirium.eval are still in an alpha or in part even experimental state.

liquirium.connect can be considered beta. The following exchanges are currently supported:

- Binance
- Bitfinex
- Poloniex
- Coinbase
- Deribit (still incomplete)

Please note that intentionally only a small part of the operations provided by the exchanges are supported.

## Roadmap/Planned features

- support for at least the most common technical indicators
- websocket support for all supported exchanges
- simple bot abstractions for single or multi-market bots
- more convenient simulation and backtesting

## Getting help

Documentation is currently minimal. However, more examples of how Liquirium Bot and Liquirium Connect can be used
will be added shortly in the 'examples' folder.
