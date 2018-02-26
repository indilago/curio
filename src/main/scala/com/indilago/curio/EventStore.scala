package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait EventStore {
  def store(event: Event)(implicit ec: ExecutionContext): Future[Event]
}
