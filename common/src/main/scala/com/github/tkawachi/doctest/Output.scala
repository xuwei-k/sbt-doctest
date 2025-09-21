package com.github.tkawachi.doctest

import sjsonnew.BasicJsonProtocol.*
import sjsonnew.JsonFormat

case class Output(
    files: Seq[String]
) extends OutputCommon

object Output {
  implicit val formatInstance: JsonFormat[Output] = {
    caseClass1(apply, (_: Output).asTupleOption)("files")
  }
}
