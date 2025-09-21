package com.github.tkawachi.doctest

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.scalafmt.interfaces.Scalafmt

object GeneratorMain {
  private implicit class JsonStringOps(private val string: String) extends AnyVal {
    def decodeFromJsonString[A](implicit r: sjsonnew.JsonReader[A]): A = {
      val json = sjsonnew.support.scalajson.unsafe.Parser.parseUnsafe(string)
      val unbuilder = new sjsonnew.Unbuilder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      r.read(Some(json), unbuilder)
    }
  }

  def main(args: Array[String]): Unit = {
    val path =
      args
        .collectFirst { case s"--input=${path}" => path }
        .getOrElse(throw new IllegalArgumentException(args.mkString(" ")))
    val str = new String(Files.readAllBytes(new File(path).toPath), StandardCharsets.UTF_8)
    val input = str.decodeFromJsonString[Input]

    Seq(
      input.scaladocSources.flatMap { f =>
        ScaladocTestGenerator(
          new File(input.base + "/" + f),
          input.encoding,
          input.testGen,
          input.decodeHtml,
          input.onlyCodeBlocksMode,
          scala.meta.dialects.Scala3 // TODO input.dialect
        )
      },
      input.markdownSource.flatMap { case (f, index) =>
        MarkdownTestGenerator(
          f,
          new File(input.markdownRelativeTo).toPath,
          input.testGen,
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
          val scalafmtInstance = input.scalafmtConfig.map { conf =>
            val scalafmtConf = new File(".scalafmt.conf")
            writeString(scalafmtConf.toPath, conf)
            writeString(writeFile.toPath, result.testSource)
            Scalafmt
              .create(this.getClass.getClassLoader)
              .createSession(scalafmtConf.toPath)
          }

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
      .toSeq
  }

  private def writeString(path: Path, str: String): Unit =
    Files.write(path, str.getBytes(StandardCharsets.UTF_8))
}
