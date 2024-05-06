package io.liquirium.eval

case object DependencyGraph {

  def empty[T]: DependencyGraph[T] = DependencyGraph[T](dependencies = Map(), dependents = Map())

}

case class DependencyGraph[T](dependencies: Map[T, Set[T]], dependents: Map[T, Set[T]]) {

  def setNodeDependencies(node: T, nodeDependencies: Set[T]): DependencyGraph[T] = {
    val dependenciesToRemove = getDependencies(node) -- nodeDependencies
    val graphAfterRemoval = dependenciesToRemove.foldLeft(this) { (g, d) => g.remove(node, d) }
    val dependenciesToAdd = nodeDependencies -- getDependencies(node)
    dependenciesToAdd.foldLeft(graphAfterRemoval) { (g, d) => g.add(node, d) }
  }

  def add(node: T, dependency: T): DependencyGraph[T] =
    copy(
      dependencies = dependencies.updated(node, dependencies.getOrElse(node, Set()) + dependency),
      dependents = dependents.updated(dependency, dependents.getOrElse(dependency, Set()) + node)
    )

  def remove(node: T, dependency: T): DependencyGraph[T] = {
    val newNodeDependencies = dependencies.getOrElse(node, Set()) - dependency
    val newDependencies =
      if (newNodeDependencies.isEmpty) dependencies - node
      else dependencies.updated(node, newNodeDependencies)
    val newDependentNodes = dependents.getOrElse(dependency, Set()) - node
    val newDependents =
      if (newDependentNodes.isEmpty) dependents - dependency
      else dependents.updated(dependency, newDependentNodes)
    copy(dependencies = newDependencies, dependents = newDependents)
  }

  def dropUnusedRecursively(node: T): (DependencyGraph[T], Iterable[T]) =
    if (dependents.contains(node)) (this, Set())
    else {
      val z = (removeDependenciesOf(node), Set(node))
      getDependencies(node).foldLeft(z) {
        case ((g, dropped), d) =>
          val (newG, newDropped) = g.dropUnusedRecursively(d)
          (newG, dropped ++ newDropped)
      }
    }

  def removeDependenciesOf(node: T): DependencyGraph[T] = {
    getDependencies(node).foldLeft(this) { (g, d) => g.remove(node, d)}
  }

  def isDependedOn(node: T): Boolean = dependents.contains(node)

  def getDependencies(node: T): Set[T] = dependencies.getOrElse(node, Set())

  def getDirectDependents(node: T): Set[T] = dependents.getOrElse(node, Set())

  def getAllDependents(node: T): Set[T] = {
    def getDependentsRecursively(node: T, acc: Set[T]): Set[T] = {
      val directDependents = getDirectDependents(node)
      val newAcc = acc ++ directDependents
      directDependents.foldLeft(newAcc) { (a, d) => getDependentsRecursively(d, a) }
    }
    getDependentsRecursively(node, Set())
  }

}
