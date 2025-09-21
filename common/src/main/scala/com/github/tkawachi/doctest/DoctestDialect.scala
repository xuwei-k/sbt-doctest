package com.github.tkawachi.doctest

import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder

sealed abstract class DoctestDialect(val value: String) extends Product with Serializable

object DoctestDialect {
  case object Scala212 extends DoctestDialect("Scala212")
  case object Scala213 extends DoctestDialect("Scala213")
  case object Scala212Source3 extends DoctestDialect("Scala212Source3")
  case object Scala213Source3 extends DoctestDialect("Scala213Source3")
  case object Scala3 extends DoctestDialect("Scala3")

  val values: Seq[DoctestDialect] = Seq(
    Scala212,
    Scala212Source3,
    Scala213,
    Scala213Source3,
    Scala3
  )

  implicit val formatInstance: JsonFormat[DoctestDialect] =
    new JsonFormat[DoctestDialect] {
      override def write[J](obj: DoctestDialect, builder: Builder[J]): Unit =
        builder.writeString(obj.value)

      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): DoctestDialect = {
        jsOpt
          .flatMap { x =>
            val str = unbuilder.readString(x)
            DoctestDialect.values.find(_.value == str)
          }
          .getOrElse(sys.error("not found"))
      }
    }

}
