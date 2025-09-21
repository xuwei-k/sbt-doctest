package com.github.tkawachi.doctest

import java.io.File
import sjsonnew.BasicJsonProtocol.*
import sjsonnew.JsonFormat

case class Input(
    base: String,
    scaladocSources: Seq[String],
    encoding: String,
    testGen: TestGenType,
    decodeHtml: Boolean,
    onlyCodeBlocksMode: Boolean,
    dialect: DoctestDialect,
    markdownSource: Seq[(File, Int)],
    markdownRelativeTo: String,
    scalafmtConfig: Option[String],
    testDir: File
) extends InputCommon

object Input {
  implicit val formatInstance: JsonFormat[Input] =
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
