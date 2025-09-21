package com.github.tkawachi.doctest

trait InputCommon { self: Input =>
  def asTupleOption = Option(Tuple.fromProductTyped(this))
}
