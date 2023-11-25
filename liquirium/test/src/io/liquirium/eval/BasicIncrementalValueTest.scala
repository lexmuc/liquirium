package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest

class BasicIncrementalValueTest extends BasicTest {

  protected trait TestValue extends BasicIncrementalValue[Int, TestValue] {

    override def inc(n: Int): TestValue = AddInt(n, this)

  }

  protected case class Root(n: Int) extends TestValue {

    override def prev: Option[TestValue] = None

    override def lastIncrement: Option[Int] = None

  }

  protected case class AddInt(n: Int, previousValue: TestValue) extends TestValue {

    override def prev: Option[TestValue] = Some(previousValue)

    override def lastIncrement: Option[Int] = Some(n)

  }

}
