package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait KeyedMaterializer[M] {
  def apply(event: Event)(implicit ec: ExecutionContext): Future[Option[M]]
}
