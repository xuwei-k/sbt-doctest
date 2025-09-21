package com.github.tkawachi.doctest

import java.io.File
import java.nio.file.Path
import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder

case class Input(
    base: String,
    scaladocSources: Seq[String],
    encoding: String,
    testGen: TestGen,
    decodeHtml: Boolean,
    onlyCodeBlocksMode: Boolean,
    dialect: String,
    markdownSource: Seq[(File, Int)],
    markdownRelativeTo: Path,
    scalafmtConfig: Option[String],
    testDir: File
) extends InputCommon

object Input {
  implicit val formatInstance: JsonFormat[Input] = {
    import sjsonnew.BasicJsonProtocol.*
    implicit val testGen: JsonFormat[TestGen] =
      new JsonFormat[TestGen] {
        override def write[J](obj: TestGen, builder: Builder[J]): Unit =
          builder.writeString(obj.value)

        override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): TestGen = {
          jsOpt
            .flatMap { x =>
              val str = unbuilder.readString(x)
              TestGen.values.find(_.value == str)
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
