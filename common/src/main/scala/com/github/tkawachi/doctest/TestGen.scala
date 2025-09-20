package com.github.tkawachi.doctest

trait TestGen

object TestGen {
  object MicroTestGen extends TestGen
  object MinitestGen extends TestGen
  object MunitGen extends TestGen
  object ScalaCheckGen extends TestGen
  sealed trait ScalaTestGen extends TestGen
  object ScalaTest30Gen extends ScalaTestGen
  object ScalaTest31Gen extends ScalaTestGen
  object Specs2TestGen extends TestGen
}
