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

- Binance (spot markets, no websocket support yet)
- Bitfinex (work in progress)
- Poloniex (work in progress)
- Coinbase (work in progress
- Deribit (work in progress)

Please note that intentionally only a small part of the operations provided by the exchanges are supported.

## Roadmap/Planned features

- support for at least the most common technical indicators
- websocket support for all supported exchanges
- simple bot abstractions for single or multi-market bots
- more convenient simulation and backtesting
- examples of how to use Liquirium from Java

## Getting started

You need Scala 2.12.18 to build and run Liquirium. The project is built with Mill (https://www.lihaoyi.com/mill/).
If you don't have Mill installed, you can just download [the in-repo bootstrap
script](https://mill-build.com/mill/Installation_IDE_Support.html#_mills_bootstrap_script_linuxos_x_only) for a 
quick start. 

Run the following command to build the project and publish it to your local ivy2 repository
(type `./mill` instead of `mill` if you haven't installed Mill globally):

```bash
mill liquirium.publishLocal
```

In the `liquirium-examples` folder you find a few examples of how to use the framework. The examples should give you
a good starting point for developing your own trading bots.

The `CandleBasedPriceTicker` example demonstrates how to use liquirium.connect to subscribe to candlestick
data from an exchange and print the latest price to the console. Run the following command to try it out:

```bash
mill liquirium-examples.runTicker
```

The `DollarCostAverageStrategy` demonstrates how to use the framework to implement a simple trading strategy without 
having to worry about placing and cancelling orders. The framework takes care of all the low-level details such as
cancelling outdated orders or retrying failed requests.

Run the following command to simulate/backtest the bot:

```bash
mill liquirium-examples.runSimulation
```


## Contact

In case you need help or if you want to give us feedback, don't hesitate to write an email to 
[hello@liquirium.io](mailto:hello@liquirium.io).
