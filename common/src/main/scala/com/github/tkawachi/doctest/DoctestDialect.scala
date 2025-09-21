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
    DoctestSjsonNewUtil.formatFromString[DoctestDialect](
      str => DoctestDialect.values.find(_.value == str),
      _.value
    )
}
