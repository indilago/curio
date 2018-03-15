package com.indilago.curio

import java.time.Instant
import java.util.UUID

import play.api.libs.json._

case class Event(id: UUID, kind: String, payload: JsValue, receiveTime: Instant)

object Event {
  def create(kind: String, payload: JsValue): Event =
    Event(UUID.randomUUID(), kind, payload, Instant.now)

  implicit val jsonFormat: Format[Event] = Json.format[Event]
}

