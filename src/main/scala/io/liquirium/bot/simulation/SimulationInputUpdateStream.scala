package io.liquirium.bot.simulation

import io.liquirium.eval.{Input, InputRequest, InputUpdate, UnknownInputsException}

import java.time.Instant

case class SimulationInputUpdateStream(
  timedInputStreams: Map[Input[_], Stream[(Instant, Any)]],
  singleInputStreamProvider: SingleInputUpdateStreamProvider,
) {

  private val nonEmptyStreams = timedInputStreams.filter(_._2.nonEmpty)

  private val nextTime: Option[Instant] =
    if (nonEmptyStreams.isEmpty) None
    else Some(nonEmptyStreams.values.iterator.map(_.head._1).min)

  val nextInputUpdate: Option[InputUpdate] =
    nextTime map { nt =>
      InputUpdate(
        nonEmptyStreams.collect {
          case (input, (t, v) #:: _) if t == nt => (input, v)
        }
      )
    }

  def advance: SimulationInputUpdateStream =
    nextTime match {
      case None => throw new RuntimeException("Trying to advance a depleted SimulationInputUpdateStream")
      case Some(nt) => copy(
        timedInputStreams = timedInputStreams map {
          case (input, (t, _) #:: tail) if t == nt => (input, tail)
          case x@_ => x
        }
      )
    }

  def processInputRequest(inputRequest: InputRequest): SimulationInputUpdateStream = nextTime match {

    case None =>
      throw new RuntimeException("Cannot process input requests when SimulationInputUpdateStream is depleted.")

    case Some(nt) =>
      val suppliedStreams = inputRequest.inputs.map { i =>
        (i, singleInputStreamProvider.getInputStream(i, nt))
      }
      val unknownInputs = suppliedStreams.filter(_._2.isEmpty).map(_._1)
      if (unknownInputs.isEmpty) {
        suppliedStreams.foldLeft(this) { case (s, (i, sup)) => s.addStream(i, sup.get) }
      }
      else {
        throw UnknownInputsException(unknownInputs)
      }

  }

  private def addStream(i: Input[_], s: Stream[(Instant, Any)]) = copy(
    timedInputStreams = timedInputStreams.updated(i, s)
  )

}
