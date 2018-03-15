package com.indilago.curio

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

abstract class BaseTest extends FlatSpec with Matchers with MockitoSugar with ScalaFutures
