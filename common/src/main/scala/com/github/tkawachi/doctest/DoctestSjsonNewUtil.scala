package com.github.tkawachi.doctest

import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder
import sjsonnew.support.scalajson.unsafe.CompactPrinter

private[doctest] object DoctestSjsonNewUtil {
  implicit class JsonStringOps(private val string: String) extends AnyVal {
    def decodeFromJsonString[A](implicit r: sjsonnew.JsonReader[A]): A = {
      val json = sjsonnew.support.scalajson.unsafe.Parser.parseUnsafe(string)
      val unbuilder = new sjsonnew.Unbuilder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      r.read(Some(json), unbuilder)
    }
  }

  implicit class JsonOps[A](private val self: A) extends AnyVal {
    def toJsonString(implicit w: sjsonnew.JsonWriter[A]): String = {
      val builder = new sjsonnew.Builder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      w.write(self, builder)
      CompactPrinter.apply(
        builder.result.getOrElse(sys.error("invalid json"))
      )
    }
  }

  def formatFromString[A](readFunction: String => Option[A], writeFunction: A => String): JsonFormat[A] =
    new JsonFormat[A] {
      override def write[J](obj: A, builder: Builder[J]): Unit =
        builder.writeString(writeFunction(obj))

      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = {
        jsOpt
          .flatMap { x =>
            readFunction(unbuilder.readString(x))
          }
          .getOrElse(sys.error("not found"))
      }
    }

}
