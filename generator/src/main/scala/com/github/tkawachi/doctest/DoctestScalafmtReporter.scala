package com.github.tkawachi.doctest

import org.scalafmt.interfaces.ScalafmtReporter

private final class DoctestScalafmtReporter extends ScalafmtReporter {
  def downloadOutputStreamWriter(): java.io.OutputStreamWriter =
    new java.io.OutputStreamWriter(scala.Console.out)
  def downloadWriter(): java.io.PrintWriter =
    new java.io.PrintWriter(scala.Console.out)
  def error(file: java.nio.file.Path, message: String): Unit =
    Console.err.println(s"error: ${file}: ${message}")
  def error(file: java.nio.file.Path, e: Throwable): Unit = {
    Console.err.println(s"error: ${file}: ${e}")
    e.printStackTrace()
  }
  def excluded(filename: java.nio.file.Path): Unit =
    println(s"file excluded: $filename")
  def parsedConfig(config: java.nio.file.Path, scalafmtVersion: String): Unit =
    ()
}
