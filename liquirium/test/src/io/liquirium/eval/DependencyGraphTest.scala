package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest

class DependencyGraphTest extends BasicTest {

  private val emptyGraph = DependencyGraph.empty[Int]

  test("the empty graph returns the empty dependencies and set of nodes depending on a node for a given node") {
    emptyGraph.getDependencies(1) shouldEqual Set()
    emptyGraph.getDirectDependents(2) shouldEqual Set()
  }

  test("added dependencies can be obtained by the depending node") {
    val g = emptyGraph.add(1, 2).add(1, 3)
    g.getDependencies(1) shouldEqual Set(2, 3)
  }

  test("dependencies of a node can be set at once") {
    val g = emptyGraph.add(1, 2)
    g.setNodeDependencies(1, Set(3, 4)) shouldEqual emptyGraph.add(1, 3).add(1, 4)
  }

  test("single dependencies can be removed") {
    val g = emptyGraph.add(1, 2).add(1, 3)
    g.remove(1, 3) shouldEqual emptyGraph.add(1, 2)
  }

  test("if the last dependency is removed for a node it's like the node had never existed") {
    emptyGraph.add(1, 2).add(1, 3).remove(1, 2).remove(1, 3) shouldEqual emptyGraph
  }

  test("only nodes being dependencies of others are depended on") {
    val g = emptyGraph.add(1, 2).add(1, 3)
    g.isDependedOn(1) shouldBe false
    g.isDependedOn(2) shouldBe true
    g.isDependedOn(3) shouldBe true
  }

  test("dependencies of a single node can be removed") {
    emptyGraph.add(1, 2).removeDependenciesOf(1).getDependencies(1) shouldEqual Set()
  }

  test("a node is not depended on anymore only if the last dependency in which it occurs is removed") {
    val g = emptyGraph.add(1, 10).add(2, 10).add(1, 11)
    g.remove(1, 10).isDependedOn(10) shouldBe true
    g.remove(1, 10).removeDependenciesOf(2).isDependedOn(10) shouldBe false
  }

  test("nodes depending on a given node can be obtained and are updated upon removal of dependencies") {
    val g = emptyGraph.add(1, 10).add(2, 10).add(3, 11)
    g.getDirectDependents(10) shouldEqual Set(1, 2)
    g.remove(1, 10).getDirectDependents(10) shouldEqual Set(2)
  }

  test("the set of all (including indirect) dependents of a node can be obtained") {
    val g = emptyGraph.add(1, 10).add(2, 10).add(3, 11).add(10, 100).add(100, 1000)
    g.getAllDependents(1000) shouldEqual Set(100, 10, 1, 2)
  }

  test("unused nodes can be dropped recursively") {
    val g = emptyGraph.add(1, 11).add(1, 12).add(2, 11).add(12, 101).add(12, 102)
    val (newGraph, _) = g.dropUnusedRecursively(1)
    newGraph shouldEqual emptyGraph.add(2, 11)
  }

  test("dropping unused nodes recursively yields a collection of the dropped nodes") {
    val g = emptyGraph.add(1, 11).add(1, 12).add(2, 11).add(12, 101).add(12, 102)
    val (_, droppedNodes) = g.dropUnusedRecursively(1)
    droppedNodes shouldEqual Set(1, 12, 101, 102)
  }

}
