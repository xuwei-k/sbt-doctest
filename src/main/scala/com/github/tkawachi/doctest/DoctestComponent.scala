package com.github.tkawachi.doctest

sealed abstract class DoctestComponent(val content: String, val lineNo: Option[Int])
case class Verbatim(code: String) extends DoctestComponent(code, None)
case class Example(expr: String, expected: TestResult, lineNo: Int) extends DoctestComponent(expr, Some(lineNo))
case class Property(prop: String, lineNo: Int) extends DoctestComponent(prop, Some(lineNo))
