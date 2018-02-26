package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait Reducer {
  def apply(event: Event)(implicit ec: ExecutionContext): Future[_]
}
