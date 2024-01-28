package io.liquirium.bot.simulation

sealed trait ChartDataSeriesAppearance

case class HistogramAppearance(
  color: String,
) extends ChartDataSeriesAppearance

case class LineAppearance(
  lineWidth: Int,
  color: String,
  overlay: Boolean,
) extends ChartDataSeriesAppearance
