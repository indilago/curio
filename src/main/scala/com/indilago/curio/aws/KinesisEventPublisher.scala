package com.indilago.curio.aws

import com.amazonaws.regions.{Region, Regions}
import com.gilt.gfc.aws.kinesis.client.{KinesisPublisher, KinesisPublisherBatchResult}
import com.indilago.curio.Event
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

class KinesisEventPublisher(
  streamName: String,
  region: Regions
) extends LazyLogging {

  private val publisher = KinesisPublisher(awsRegion = Some(Region.getRegion(region)))

  def publish(event: Event)(implicit ec: ExecutionContext): Future[_] = {
    logger.debug("Publishing $event")
    publisher.publishBatch(streamName, Seq(event)).map { result: KinesisPublisherBatchResult =>
      if (result.successRecordCount < 1)
        throw new Exception(s"Failed to publish event: ${result.errorCodes}")
    }
  }
}
