package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait EventProcessor {
  def process(event: Event)(implicit ec: ExecutionContext): Future[_]
}
