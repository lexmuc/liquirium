package io.liquirium.core

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.asset

class AssetMarketTest extends BasicTest {

  test("an asset market can be inverted") {
    AssetMarket(asset("AAA"), asset("BBB")).invert shouldEqual AssetMarket(asset("BBB"), asset("AAA"))
  }

}
