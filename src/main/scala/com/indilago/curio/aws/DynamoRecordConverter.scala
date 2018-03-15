package com.indilago.curio.aws

import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait DynamoRecordConverter[M, K] {
  def toKey(key: K): Map[String, AttributeValue]
  def toModel(attributes: Map[String, AttributeValue]): M
  def toAttributes(model: M): Map[String, AttributeValue]
}
