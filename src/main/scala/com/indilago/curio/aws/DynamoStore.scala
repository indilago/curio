package com.indilago.curio.aws

import java.util.NoSuchElementException

import akka.actor.ActorSystem
import akka.stream.alpakka.dynamodb.impl.DynamoSettings
import akka.stream.alpakka.dynamodb.scaladsl.DynamoClient
import akka.stream.alpakka.dynamodb.scaladsl.DynamoImplicits._
import akka.stream.{ActorMaterializer, Materializer}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, PutItemRequest}
import com.indilago.curio.ViewStore

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class DynamoStore[M, K](system: ActorSystem, converter: DynamoRecordConverter[M, K], tableName: String) extends ViewStore[M, K] {
  implicit val _system: ActorSystem = system
  implicit val _mat: Materializer = ActorMaterializer()

  val settings = DynamoSettings(system)
  val client = DynamoClient(settings)
  import converter._

  override def find(key: K)(implicit ec: ExecutionContext): Future[Option[M]] = {
    val keyAttributes = toKey(key).asJava
    client.single(new GetItemRequest(tableName, keyAttributes))
      .map { result =>
        result.getItem match {
          case attributes: java.util.Map[String, AttributeValue] =>
            Some(toModel(attributes.asScala.toMap))
          case _ => println(s"UNDERSCORE"); None
          case null => println(s"NULL"); None
        }
      }
      .recover {
        case _: NoSuchElementException =>
          println(s"NoSuchElementException")
          None
        case e =>
          println(s"DynamoError: ${e.getMessage} $e")
          throw e
      }
  }

  override def put(model: M)(implicit ec: ExecutionContext): Future[M] = {
    val attributes = toAttributes(model).asJava
    client.single(new PutItemRequest(tableName, attributes))
      .map(_ => model)
  }
}
