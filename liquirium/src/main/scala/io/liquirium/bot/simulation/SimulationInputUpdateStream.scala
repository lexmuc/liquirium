package io.liquirium.bot.simulation

import io.liquirium.eval.{Input, InputRequest, InputUpdate, UnknownInputsException}

import java.time.Instant


trait SimulationInputUpdateStream {

  def currentInputUpdate: Option[InputUpdate]

  def advance: SimulationInputUpdateStream

  def processInputRequest(inputRequest: InputRequest): SimulationInputUpdateStream

}


object SimulationInputUpdateStream {

  def apply(
    start: Instant,
    end: Instant,
    singleInputStreamProvider: SingleInputUpdateStreamProvider,
  ): SimulationInputUpdateStream = Impl(
    nextInputUpdateWithTime = Some((start, InputUpdate(Map()))),
    timedInputStreamsByInput = Map(),
    end = end,
    singleInputStreamProvider = singleInputStreamProvider,
  )

  private case class Impl(
    nextInputUpdateWithTime: Option[(Instant, InputUpdate)],
    timedInputStreamsByInput: Map[Input[_], Stream[(Instant, Any)]],
    end: Instant,
    singleInputStreamProvider: SingleInputUpdateStreamProvider,
  ) extends SimulationInputUpdateStream {

    private def time: Option[Instant] = nextInputUpdateWithTime.map(_._1)

    override def currentInputUpdate: Option[InputUpdate] = nextInputUpdateWithTime.map(_._2)

    private def nextTime: Option[Instant] =
      if (timedInputStreamsByInput.isEmpty) None
      else Some(timedInputStreamsByInput.values.map(_.head._1).min)

    def advance: SimulationInputUpdateStream =
      if (time.isEmpty) {
        throw new RuntimeException("Trying to advance a depleted SimulationInputUpdateStream")
      }
      else {
        nextTime match {
          case None => copy(
            nextInputUpdateWithTime = None,
          )

          case Some(nt) =>
            val newUpdate = InputUpdate(
              timedInputStreamsByInput.collect {
                case (input, (t, v) #:: _) if t == nt => (input, v)
              }
            )
            copy(
              nextInputUpdateWithTime = Some((nt, newUpdate)),
              timedInputStreamsByInput =
                timedInputStreamsByInput.mapValues { s =>
                    if (s.head._1 == nt) s.tail else s
                  }
                  .filter(_._2.nonEmpty)
            )
        }
      }

    def processInputRequest(inputRequest: InputRequest): SimulationInputUpdateStream = time match {

      case None =>
        throw new RuntimeException("Cannot process input requests when SimulationInputUpdateStream is depleted.")

      case Some(t) =>
        val suppliedStreams = inputRequest.inputs.map { i =>
          (i, singleInputStreamProvider.getInputStream(i, start = t, end = end))
        }
        val unknownInputs = suppliedStreams.filter(_._2.isEmpty).map(_._1)
        if (unknownInputs.isEmpty) {
          suppliedStreams.foldLeft(this) { case (s, (i, sup)) => s.addStream(i, sup.get) }
        }
        else {
          throw UnknownInputsException(unknownInputs)
        }

    }

    private def addStream(i: Input[_], s: Stream[(Instant, Any)]) =
      s.headOption match {
        case Some((t, _)) if t != time.get =>
          throw new RuntimeException("Trying to add a stream that does not start at the current time")

        case Some((t, v)) if t == time.get =>
          val newStreamsByInput =
            if (s.tail.isEmpty) timedInputStreamsByInput
            else timedInputStreamsByInput.updated(i, s.tail)
          copy(
            timedInputStreamsByInput = newStreamsByInput,
            nextInputUpdateWithTime = Some(time.get, InputUpdate(currentInputUpdate.get.updateMappings.updated(i, v))),
          )

        case None =>
          throw new RuntimeException("Trying to add an empty stream for input " + i)

      }

  }

}
