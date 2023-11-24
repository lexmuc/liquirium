package io.liquirium.core

import io.liquirium.util.AbsoluteQuantity

sealed trait OperationRequestSuccessResponse[+A <: OperationRequest]

final case class OrderRequestConfirmation(order: Option[Order], immediateTrades: Seq[Trade])
  extends OperationRequestSuccessResponse[OrderRequest]

final case class CancelRequestConfirmation(absoluteRestQuantity: Option[AbsoluteQuantity])
  extends OperationRequestSuccessResponse[CancelRequest]
