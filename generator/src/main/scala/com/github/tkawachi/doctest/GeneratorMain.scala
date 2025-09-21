package com.github.tkawachi.doctest

import com.github.tkawachi.doctest.DoctestSjsonNewUtil.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.scalafmt.interfaces.Scalafmt
import scala.meta.dialects

object GeneratorMain {

  def main(args: Array[String]): Unit = {
    val inputPath =
      args
        .collectFirst { case s"--input=${path}" => path }
        .getOrElse(throw new IllegalArgumentException(args.mkString(" ")))
    val str = new String(Files.readAllBytes(new File(inputPath).toPath), StandardCharsets.UTF_8)
    val input = str.decodeFromJsonString[Input]
    val outputPath =
      args
        .collectFirst { case s"--output=${path}" => path }
        .getOrElse(throw new IllegalArgumentException(args.mkString(" ")))

    val testGen = {
      input.testGen match {
        case TestGenType.MicroTest =>
          MicroTestGen
        case TestGenType.Minitest =>
          MinitestGen
        case TestGenType.Munit =>
          MunitGen
        case TestGenType.ScalaCheck =>
          ScalaCheckGen
        case TestGenType.ScalaTest30 =>
          ScalaTest30Gen
        case TestGenType.ScalaTest31 =>
          ScalaTest31Gen
        case TestGenType.Specs2 =>
          Specs2TestGen
      }
    }
    val scalafmtInstance = input.scalafmtConfig.map { conf =>
      val scalafmtConf = new File(".scalafmt.conf")
      writeString(scalafmtConf.toPath, conf)
      Scalafmt
        .create(this.getClass.getClassLoader)
        .withReporter(new DoctestScalafmtReporter)
        .createSession(scalafmtConf.toPath)
    }

    val files = Seq(
      input.scaladocSources.flatMap { f =>
        ScaladocTestGenerator(
          new File(input.base + "/" + f),
          input.encoding,
          testGen,
          input.decodeHtml,
          input.onlyCodeBlocksMode,
          input.dialect match {
            case DoctestDialect.Scala212 =>
              dialects.Scala212
            case DoctestDialect.Scala213 =>
              dialects.Scala213
            case DoctestDialect.Scala212Source3 =>
              dialects.Scala212Source3
            case DoctestDialect.Scala213Source3 =>
              dialects.Scala213Source3
            case DoctestDialect.Scala3 =>
              dialects.Scala3
          }
        )
      },
      input.markdownSource.flatMap { case (f, index) =>
        MarkdownTestGenerator(
          f,
          new File(input.markdownRelativeTo).toPath,
          testGen,
          index.toString
        )
      }
    ).flatten
      .groupBy(r => r.pkg -> r.basename)
      .flatMap { case ((pkg, basename), results) =>
        results.zipWithIndex.map { case (result, idx) =>
          val writeBasename = if (idx == 0) basename else basename + idx
          val writeDir = pkg.fold(input.testDir)(_.split("\\.").foldLeft(input.testDir) { (a: File, e: String) =>
            new File(a, e)
          })
          val writeFile = new File(writeDir, writeBasename + "Doctest.scala")
          writeString(writeFile.toPath, result.testSource)

          scalafmtInstance.foreach { fmt =>
            writeString(
              writeFile.toPath,
              fmt.format(
                writeFile.toPath,
                scala.io.Source.fromFile(writeFile, "UTF-8").getLines().mkString("\n")
              )
            )
          }
          writeFile
        }
      }
      .toList

    writeString(
      new File(outputPath).toPath,
      Output(files.map(_.getCanonicalPath)).toJsonString
    )
  }

  private def writeString(path: Path, str: String): Unit = {
    Files.createDirectories(path.toAbsolutePath.getParent)
    Files.write(path, str.getBytes(StandardCharsets.UTF_8))
  }
}
