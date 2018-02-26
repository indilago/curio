package com.indilago.curio

import scala.concurrent.{ExecutionContext, Future}

trait ViewStore[M, K] {
  def find(key: K)(implicit ec: ExecutionContext): Future[Option[M]]
  def require(key: K)(implicit ec: ExecutionContext): Future[M] =
    find(key).map {
      case Some(model: M) => model
      case None => throw new Exception(s"View not found for $key")
    }
  def put(model: M)(implicit ec: ExecutionContext): Future[_]
}
