package io.liquirium.eval

import io.liquirium.eval.IncrementalFoldHelpers.IncrementalMapEval
import io.liquirium.eval.helpers.EvalTest

class IncrementalFoldHelpersTest_FilterValues extends EvalTest {

  case class TestMapInput(n: Int) extends Input[IncrementalMap[String, Int]]
  private val input = TestMapInput(1)
  private var inputMap = IncrementalMap.empty[String, Int]

  private var context: UpdatableContext = IncrementalContext()
  private val metric = InputEval(input).filterValuesIncremental(_ % 2 == 0)

  private val emptyMap = IncrementalMap.empty[String, Int]

  private def eval(): IncrementalMap[String, Int] = {
    context = context.update(InputUpdate(Map(
      input -> inputMap
    )))
    val (evaluationResult, newContext) = context.evaluate(metric)
    context = newContext
    evaluationResult match {
      case Value(v) => v
      case _ => throw new RuntimeException("input request not expected... all inputs were supplied")
    }
  }

  test("new values are only added when they match the predicate") {
    eval() shouldEqual emptyMap
    inputMap = inputMap.update("A", 1).update("B", 2).update("C", 3)
    eval() shouldEqual emptyMap.update("B", 2)
  }

  test("when values are removed they are also removed from the filtered map") {
    val completeMap = inputMap.update("A", 2).update("B", 4).update("C", 6)
    inputMap = completeMap
    eval()
    inputMap = completeMap.deleteKey("B").deleteKey("A")
    eval().mapValue shouldEqual Map("C" -> 6)
  }

  test("values are only deleted if they are present") {
    val completeMap = inputMap.update("A", 2).update("B", 3).update("C", 4)
    inputMap = completeMap
    val firstEval = eval()
    inputMap = completeMap.deleteKey("A").deleteKey("B")
    val secondEval = eval()
    secondEval.incrementsAfter(firstEval) shouldEqual List("A" -> None)
  }

  test("values are deleted when an updated value does not match anymore") {
    val completeMap = inputMap.update("A", 2).update("B", 3).update("C", 4)
    inputMap = completeMap
    val firstEval = eval()
    inputMap = completeMap.update("A", 3)
    val secondEval = eval()
    secondEval.incrementsAfter(firstEval) shouldEqual List("A" -> None)
  }

}
