package com.github.tkawachi.doctest

trait InputCommon { self: Input =>
  def asTupleOption = Input.unapply(this)
}
