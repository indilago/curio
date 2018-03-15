package com.indilago.curio.aws

import java.time.Instant
import java.util.UUID

import com.gilt.gfc.aws.kinesis.client.{KinesisPublisher, KinesisPublisherBatchResult, KinesisRecordWriter}
import com.indilago.curio.{BaseTest, Event}
import org.mockito.Mockito._
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class KinesisEventPublisherTest extends BaseTest {

  val publisher = mock[KinesisPublisher](RETURNS_SMART_NULLS)
  val streamName = "testStreamName"

  val sut = new KinesisEventPublisher(publisher, streamName)

  val now = Instant.now()

  val payloadStr = """{"hello":"world"}"""
  val payload = Json.parse(payloadStr)

  import KinesisEventPublisher.EventWriter

  def makeEvent(): Event = {
    Event(UUID.randomUUID(), "TestEvent-1.0", payload, now)
  }

  "publisher" should "send a single message to Kinesis" in {
    val event = makeEvent()
    val result = new KinesisPublisherBatchResult(
      successRecordCount = 1
    )

    when(publisher.publishBatch(streamName, Seq(event)))
      .thenReturn(Future.successful(result))

    sut.publish(event)
    verify(publisher).publishBatch(streamName, Seq(event))
  }

  it should "fail if no records were published" in {
    val event = makeEvent()
    val result = new KinesisPublisherBatchResult(
      successRecordCount = 0
    )

    when(publisher.publishBatch(streamName, Seq(event)))
      .thenReturn(Future.successful(result))

    sut.publish(event).failed.futureValue shouldBe an[Exception]

    verify(publisher).publishBatch(streamName, Seq(event))
  }
}
