package com.indilago.curio

import java.time.Instant
import java.util.UUID

import play.api.libs.json.{Format, JsValue, Json}

case class Event(id: UUID, kind: String, payload: JsValue, receiveTime: Instant)

object Event {
  def create(kind: String, payload: JsValue): Event =
    Event(UUID.randomUUID(), kind, payload, Instant.now)

  implicit val jsonReads: Format[Event] = Json.format[Event]
}
