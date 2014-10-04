package com.github.tkawachi.doctest

import java.io.File
import xsbti.{Maybe, Position}
import sbt.Logger.o2m
import scala.io.Source
import org.apache.commons.io.FilenameUtils

object TestGenerator {

  case class Result(pkg: Option[String], basename: String, testSource: String, srcFile: File, examples: Seq[ParsedDoctest]) {
    def positionMappers: Seq[xsbti.Position => Option[xsbti.Position]] ={
      examples.map{ parsedTest =>
        parsedTest.components.map{
        }.toMap.lift
      }
    }
  }

  val extractor = new Extractor

  private def testGen(framework: TestFramework): TestGen = framework match {
    case ScalaTest => ScalaTestGen
    case Specs2 => Specs2TestGen
  }

  /**
   * Generates test source code from scala source file.
   */
  def apply(srcFile: File, framework: TestFramework): Seq[Result] = {
    val src = Source.fromFile(srcFile).mkString
    val basename = FilenameUtils.getBaseName(srcFile.getName)
    extractor.extract(src)
      .flatMap(comment => CommentParser(comment).right.toOption.filter(_.components.size > 0))
      .groupBy(_.pkg).map {
        case (pkg, examples) =>
          val gen = testGen(framework)
          Result(pkg, basename, gen.generate(basename, pkg, examples), srcFile, examples)
      }.toSeq
  }
}
