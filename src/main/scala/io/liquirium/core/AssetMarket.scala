package io.liquirium.core

case class AssetMarket(base: Asset, quote: Asset) {

  lazy val invert: AssetMarket = copy(base = quote, quote = base)

}
