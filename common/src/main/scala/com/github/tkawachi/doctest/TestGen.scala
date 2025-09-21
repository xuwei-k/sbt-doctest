package com.github.tkawachi.doctest

sealed abstract class TestGen(val value: String) extends Product with Serializable

object TestGen {
  case object MicroTestGen extends TestGen("micro-test")
  case object MinitestGen extends TestGen("mini-test")
  case object MunitGen extends TestGen("munit")
  case object ScalaCheckGen extends TestGen("scalacheck")
  sealed abstract class ScalaTestGen(value: String) extends TestGen(value)
  case object ScalaTest30Gen extends ScalaTestGen("scalatest30")
  case object ScalaTest31Gen extends ScalaTestGen("scalatest31")
  case object Specs2TestGen extends TestGen("specs2")

  val values: Seq[TestGen] = Seq(
    MicroTestGen,
    MicroTestGen,
    MunitGen,
    ScalaCheckGen,
    ScalaTest30Gen,
    ScalaTest31Gen,
    Specs2TestGen
  )
}
