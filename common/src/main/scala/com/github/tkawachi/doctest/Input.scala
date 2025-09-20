package com.github.tkawachi.doctest

import java.io.File
import java.nio.file.Path

case class Input(
    scaladocSources: Seq[File],
    encoding: String,
    testGen: TestGen,
    decodeHtml: Boolean,
    onlyCodeBlocksMode: Boolean,
    dialect: String,
    markdownSource: Seq[(File, Int)],
    markdownRelativeTo: Path,
    scalafmtConfig: Option[String]
)
