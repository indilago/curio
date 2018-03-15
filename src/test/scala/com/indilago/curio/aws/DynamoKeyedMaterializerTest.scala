package com.indilago.curio.aws

import java.time.Instant
import java.util.UUID

import com.indilago.curio.{BaseTest, Event, Reducer}
import org.mockito.Mockito._
import play.api.libs.json.{JsNumber, JsObject, Json, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DynamoKeyedMaterializerTest extends BaseTest {

  type ThingId = Long
  case class Thing(id: ThingId, pokes: Int)

  case class PokeEvent(id: ThingId)
  object PokeEvent {
    def parse(event: Event): Option[PokeEvent] =
      event.kind match {
        case "PokeEvent" =>
          Some(event.payload.validate[PokeEvent].getOrElse(throw new Exception("Failed parsing poke event")))
        case _ =>
          None
      }
    implicit val jsonReads: Reads[PokeEvent] = Json.reads[PokeEvent]
  }

  val store = mock[DynamoStore[Thing, ThingId]](RETURNS_SMART_NULLS)
  val reducer = mock[Reducer[Thing]](RETURNS_SMART_NULLS)

  def filter(event: Event): Future[Option[ThingId]] = Future {
    PokeEvent.parse(event).map(_.id)
  }

  val sut = new DynamoKeyedMaterializer[Thing, ThingId](
    store,
    reducer,
    filter
  )

  "Materializer" should "store a new record for a nonexistent view" in {
    val id: ThingId = 3
    val event = pokeEvent(id)
    val initialState = Thing(id, 1)

    when(reducer.initialState(event))
      .thenReturn(Future.successful(initialState))
    when(store.find(id))
      .thenReturn(Future.successful(None))
    when(store.put(initialState))
      .thenReturn(Future.successful(initialState))

    sut.apply(event).futureValue shouldBe Some(initialState)

    verify(store).put(initialState)
  }

  it should "update a record for an existing view" in {
    val id: ThingId = 3
    val event = pokeEvent(id)
    val state0 = Thing(id, 1)
    val state1 = Thing(id, 2)

    when(reducer.apply(state0, event))
      .thenReturn(Future.successful(state1))
    when(store.find(id))
      .thenReturn(Future.successful(Some(state0)))
    when(store.put(state1))
      .thenReturn(Future.successful(state1))

    sut.apply(event).futureValue shouldBe Some(state1)

    verify(store).put(state1)
  }

  it should "ignore irrelevant events" in {
    val event = pokeEvent(3).copy(kind = "SomeEvent")

    sut.apply(event).futureValue shouldBe None
  }


  private def pokeEvent(thingId: ThingId) =
    Event(
      id = UUID.randomUUID(),
      kind = "PokeEvent",
      payload = JsObject(Map("id" -> JsNumber(thingId))),
      receiveTime = Instant.now
    )
}
