package io.liquirium.bot.simulation

sealed trait DataSeriesAppearance

case class HistogramAppearance(
  color: String,
) extends DataSeriesAppearance

case class LineAppearance(
  lineWidth: Int,
  color: String,
  overlay: Boolean,
) extends DataSeriesAppearance
