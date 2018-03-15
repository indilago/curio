package com.indilago.curio.aws

import com.amazonaws.regions.{Region, Regions}
import com.gilt.gfc.aws.kinesis.client.{KinesisPublisher, KinesisPublisherBatchResult, KinesisRecord, KinesisRecordWriter}
import com.indilago.curio.Event
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class KinesisEventPublisher(
  publisher: KinesisPublisher,
  streamName: String
) extends LazyLogging {
  import KinesisEventPublisher._

  def publish(event: Event)(implicit ec: ExecutionContext): Future[_] = {
    logger.debug("Publishing $event")
    publisher.publishBatch(streamName, Seq(event))(EventWriter).map { result: KinesisPublisherBatchResult =>
      if (result.successRecordCount < 1)
        throw new Exception(s"Failed to publish event: ${result.errorCodes}")
    }
  }
}

object KinesisEventPublisher {
  def apply(streamName: String, region: Regions): KinesisEventPublisher = {
    val p = KinesisPublisher(awsRegion = Some(Region.getRegion(region)))
    new KinesisEventPublisher(p, streamName)
  }

  implicit object EventWriter extends KinesisRecordWriter[Event] {
    override def toKinesisRecord(event: Event) : KinesisRecord = {
      val json = Json.toJson(event)
      KinesisRecord(event.kind, Json.stringify(json).getBytes("UTF-8"))
    }
  }

}
