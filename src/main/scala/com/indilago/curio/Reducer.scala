package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait Reducer[M] {
  def initialState(event: Event)(implicit ec: ExecutionContext): Future[M]
  def apply(state: M, event: Event)(implicit ec: ExecutionContext): Future[M]
}
