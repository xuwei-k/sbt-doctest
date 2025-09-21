package com.github.tkawachi.doctest

trait OutputCommon { self: Output =>
  def asTupleOption = Option(this.files)
}
