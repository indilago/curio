package com.indilago.curio.aws

import com.indilago.curio.{Event, KeyedMaterializer, Reducer}

import scala.concurrent.{ExecutionContext, Future}

class DynamoKeyedMaterializer[M, K](
  store: DynamoStore[M, K],
  reducer: Reducer[M],
  filter: Event => Future[Option[K]]
) extends KeyedMaterializer[M] {

  override def apply(event: Event)(implicit ec: ExecutionContext): Future[Option[M]] = {
    filter(event).flatMap {
      case Some(k: K) =>
        for {
          maybeState <- store.find(k)
          reduced <- reduce(event, maybeState)
          stored <- store.put(reduced)
        } yield Option(stored)
      case None =>
        Future.successful(None)
    }
  }

  private def reduce(event: Event, maybeState: Option[M])
                    (implicit ec: ExecutionContext): Future[M] = {
    maybeState match {
      case Some(state) =>
        reducer.apply(state, event)
      case None =>
        reducer.initialState(event)
    }
  }
}