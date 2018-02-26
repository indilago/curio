package com.indilago.curio.aws

import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.indilago.curio.Event
import play.api.libs.json.Json

class DynamoEventStore(system: ActorSystem)
  extends DynamoStore[Event, UUID](system) {
  import DynamoEventStore._

  override def toKey(key: UUID): Map[String, AttributeValue] =
    Map(IdAttr -> new AttributeValue(key.toString))

  override def toModel(attributes: Map[String, AttributeValue]): Event =
    Event(
      id = UUID.fromString(attributes(IdAttr).getS),
      kind = attributes(KindAttr).getS,
      payload = Json.parse(attributes(PayloadAttr).getS),
      receiveTime = Instant.ofEpochMilli(attributes(ReceiveTimeAttr).getN.toLong)
    )

  override def toAttributes(event: Event): Map[String, AttributeValue] = Map(
    IdAttr -> new AttributeValue(event.id.toString),
    KindAttr -> new AttributeValue(event.kind),
    PayloadAttr -> new AttributeValue(event.payload.toString()),
    ReceiveTimeAttr -> new AttributeValue(event.receiveTime.toEpochMilli.toString)
  )
}

object DynamoEventStore {
  val IdAttr = "Id"
  val KindAttr = "Kind"
  val PayloadAttr = "Payload"
  val ReceiveTimeAttr = "ReceiveTime"
}