package com.github.tkawachi.doctest

import java.io.File
import sjsonnew.BasicJsonProtocol.*
import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder

case class Input(
    base: String,
    scaladocSources: Seq[String],
    encoding: String,
    testGen: TestGenType,
    decodeHtml: Boolean,
    onlyCodeBlocksMode: Boolean,
    dialect: String,
    markdownSource: Seq[(File, Int)],
    markdownRelativeTo: String,
    scalafmtConfig: Option[String],
    testDir: File
) extends InputCommon

object Input {
  implicit val formatInstance: JsonFormat[Input] = {
    implicit val testGen: JsonFormat[TestGenType] =
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

    caseClass11(Input.apply, (_: Input).asTupleOption)(
      "base",
      "scaladocSources",
      "encoding",
      "testGen",
      "decodeHtml",
      "onlyCodeBlocksMode",
      "dialect",
      "markdownSource",
      "markdownRelativeTo",
      "scalafmtConfig",
      "testDir"
    )
  }
}
