package com.indilago.curio.aws

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.alpakka.kinesis.ShardSettings
import akka.stream.alpakka.kinesis.scaladsl.KinesisSource
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.{AmazonKinesisAsync, AmazonKinesisAsyncClientBuilder}
import com.amazonaws.services.kinesis.model.{Record, ShardIteratorType}
import com.indilago.curio.{Event, EventProcessor}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.JavaConverters._


class KinesisEventConsumerWorker(processors: Seq[EventProcessor])
  extends Actor with ActorLogging {
  import KinesisEventConsumerWorker._

  // @todo
  implicit val ec: ExecutionContext = context.dispatcher

  private def toEvent(record: Record): Event = {
    val str = new String(record.getData.array)
    Json.parse(str).validate[Event].get
  }

  override def receive: Receive = {
    case record: Record =>
      implicit val timeout: Timeout = 5.seconds
      val event = toEvent(record)
      log.debug(s"Processing event: $event")
      val _sender = sender
      Future.sequence(processors.map(_.process(event))).map { _ =>
        log.debug(s"Finished processing ${event.id}")
        _sender ! Ack
      }
    case Init =>
      log.debug("EventConsumer received Init")
      sender ! Ack
    case unknown =>
      log.warning(s"EventConsumer received unknown: $unknown")
  }
}

object KinesisEventConsumerWorker {
  case class Process(event: Event)
  case class Completed(eventId: UUID)

  case object Init
  case object Ack
  case object ShutItDown

  def props(processors: Seq[EventProcessor]): Props =
    Props(new KinesisEventConsumerWorker(processors))
}

class KinesisEventConsumer(
  system: ActorSystem,
  streamName: String,
  shards: Seq[String],
  kinesisAsync: AmazonKinesisAsync,
  processors: Seq[EventProcessor]
) {
  import KinesisEventConsumerWorker._

  implicit val _system: ActorSystem = system
  implicit val _mat: Materializer = ActorMaterializer()

  private val mergeSettings = shards.map { shardId =>
    ShardSettings(
      streamName,
      shardId,
      shardIteratorType = ShardIteratorType.TRIM_HORIZON,
      refreshInterval = 1.second,
      limit = 500
    )
  }.toList

  private val source = KinesisSource.basicMerge(mergeSettings, kinesisAsync)

  private val consumer = system.actorOf(KinesisEventConsumerWorker.props(processors))

  def start(): Unit = {
    source.runWith(Sink.actorRefWithAck(
      consumer,
      onInitMessage = Init,
      ackMessage = Ack,
      onCompleteMessage = ShutItDown
    ))
  }
}

object KinesisEventConsumer {
  def apply(
    system: ActorSystem,
    streamName: String,
    shards: Seq[String],
    region: Regions,
    processors: Seq[EventProcessor]
  ): KinesisEventConsumer = {
    val kinesisAsync = AmazonKinesisAsyncClientBuilder.standard.withRegion(region).build
    new KinesisEventConsumer(system, streamName, shards, kinesisAsync, processors)
  }

  def apply(
    system: ActorSystem,
    streamName: String,
    region: Regions,
    processors: Seq[EventProcessor]
  ): KinesisEventConsumer = {
    val kinesisAsync = AmazonKinesisAsyncClientBuilder.standard.withRegion(region).build
    val shards = kinesisAsync.describeStreamAsync(streamName)
      .get().getStreamDescription.getShards
      .asScala
      .map(_.getShardId)
    new KinesisEventConsumer(system, streamName, shards, kinesisAsync, processors)
  }
}