package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.asset
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class AssetMarketTest extends BasicTest {

  test("an asset market can be inverted") {
    AssetMarket(asset("AAA"), asset("BBB")).invert shouldEqual AssetMarket(asset("BBB"), asset("AAA"))
  }

}
