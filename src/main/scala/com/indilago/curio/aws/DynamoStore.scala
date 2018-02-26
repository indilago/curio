package com.indilago.curio.aws

import akka.actor.ActorSystem
import akka.stream.alpakka.dynamodb.impl.DynamoSettings
import akka.stream.alpakka.dynamodb.scaladsl.DynamoClient
import akka.stream.alpakka.dynamodb.scaladsl.DynamoImplicits._
import akka.stream.{ActorMaterializer, Materializer}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, PutItemRequest}
import com.indilago.curio.ViewStore

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

abstract class DynamoStore[M, K](system: ActorSystem) extends ViewStore[M, K] {
  implicit val _system: ActorSystem = system
  implicit val _mat: Materializer = ActorMaterializer()

  val settings = DynamoSettings(system)
  val client = DynamoClient(settings)

  def toKey(key: K): Map[String, AttributeValue]
  def toModel(attributes: Map[String, AttributeValue]): M
  def toAttributes(model: M): Map[String, AttributeValue]

  override def find(key: K)(implicit ec: ExecutionContext): Future[Option[M]] = {
    val keyAttributes = toKey(key).asJava
    client.single(new GetItemRequest().withKey(keyAttributes)).map { result =>
      result.getItem match {
        case attributes: java.util.Map[String, AttributeValue] =>
          Some(toModel(attributes.asScala.toMap))
        case _ => None
      }
    }
  }

  override def put(model: M)(implicit ec: ExecutionContext): Future[_] = {
    val attributes = toAttributes(model).asJava
    client.single(new PutItemRequest().withItem(attributes))
  }
}
