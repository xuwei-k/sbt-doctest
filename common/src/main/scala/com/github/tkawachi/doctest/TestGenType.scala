package com.github.tkawachi.doctest

import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder

sealed abstract class TestGenType(val value: String) extends Product with Serializable

object TestGenType {
  case object MicroTest extends TestGenType("micro-test")
  case object Minitest extends TestGenType("mini-test")
  case object Munit extends TestGenType("munit")
  case object ScalaCheck extends TestGenType("scalacheck")
  sealed abstract class ScalaTestGen(value: String) extends TestGenType(value)
  case object ScalaTest30 extends ScalaTestGen("scalatest30")
  case object ScalaTest31 extends ScalaTestGen("scalatest31")
  case object Specs2 extends TestGenType("specs2")

  val values: Seq[TestGenType] = Seq(
    MicroTest,
    MicroTest,
    Munit,
    ScalaCheck,
    ScalaTest30,
    ScalaTest31,
    Specs2
  )

  implicit val formatInstance: JsonFormat[TestGenType] =
    new JsonFormat[TestGenType] {
      override def write[J](obj: TestGenType, builder: Builder[J]): Unit =
        builder.writeString(obj.value)

      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): TestGenType = {
        jsOpt
          .flatMap { x =>
            val str = unbuilder.readString(x)
            TestGenType.values.find(_.value == str)
          }
          .getOrElse(sys.error("not found"))
      }
    }

}
