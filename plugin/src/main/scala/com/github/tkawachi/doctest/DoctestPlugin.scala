package com.github.tkawachi.doctest

import java.nio.charset.StandardCharsets
import org.apache.commons.io.FilenameUtils
import sbt.*
import sbt.Keys.*
import sbt.internal.io.Source
import sbt.io.AllPassFilter
import sbt.io.NothingFilter
import sjsonnew.support.scalajson.unsafe.CompactPrinter

/**
 * Sbt plugin for doctest.
 *
 * It enables doctest like following.
 *
 * {{{
 * # In Python style
 * >>> 1 + 1
 * 2
 *
 * # In scala repl style
 * scala> 1 + 10
 * res0: Int = 11
 * }}}
 */
object DoctestPlugin extends AutoPlugin with DoctestCompat {
  self =>

  sealed abstract class DoctestTestFramework

  object DoctestTestFramework {
    case object Specs2 extends DoctestTestFramework
    case object ScalaTest extends DoctestTestFramework
    case object ScalaCheck extends DoctestTestFramework
    case object MicroTest extends DoctestTestFramework
    case object Minitest extends DoctestTestFramework
    case object Munit extends DoctestTestFramework
  }
  import DoctestTestFramework.*

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[?]] = doctestSettings

  object autoImport {
    val doctestTestFramework = settingKey[DoctestTestFramework](
      "Test framework. Specify MicroTest, Minitest, Munit, ScalaTest, ScalaCheck or Specs2."
    )
    val doctestScalaTestVersion =
      settingKey[Option[String]]("Explicitly specify ScalaTest version to generate test files (ex. Some(\"3.2.0\")).")
    val doctestMarkdownEnabled = settingKey[Boolean]("Whether to compile markdown into doctests.")
    val doctestMarkdownPathFinder = settingKey[PathFinder]("PathFinder to find markdown to test.")
    val doctestGenTests = taskKey[Seq[File]]("Generates test files.")
    val doctestDecodeHtmlEntities = settingKey[Boolean]("Whether to decode HTML entities.")
    val doctestIgnoreRegex =
      settingKey[Option[String]]("All sources that match the regex will not be used for tests generation")
    val doctestOnlyCodeBlocksMode = settingKey[Boolean]("Whether to treat all code in Scaladocs as pure code blocks.")
    val doctestDialect = settingKey[DoctestDialect]("dialect")
    val doctestScalafmt = settingKey[Boolean]("format generated code by scalafmt")

    val DoctestTestFramework = self.DoctestTestFramework
  }

  import autoImport.*

  private val supportScalaBinaryVersions: Set[String] = Set("2.12", "2.13", "3")

  private[doctest] def findEncoding(scalacOptions: Seq[String]): Option[String] = scalacOptions match {
    case Seq() => None
    case Seq(_, tail*) => scalacOptions.zip(tail).collectFirst { case ("-encoding", enc) => enc }
  }

  /**
   * Settings for test Generation.
   */
  val doctestGenSettings = Seq(
    libraryDependencies ++= {
      if (supportScalaBinaryVersions(scalaBinaryVersion.value)) {
        // https://github.com/portable-scala/sbt-platform-deps/blob/1b9d7ef512546e8/src/main/scala/org/portablescala/sbtplatformdeps/PlatformDepsPlugin.scala#L20-L21
        SettingKey[CrossVersion]("platformDepsCrossVersion").?.value match {
          case Some(c) =>
            Seq(
              ("io.github.sbt-doctest" %% "doctest-runtime" % DoctestBuildInfo.version).cross(c) % Test
            )
          case None =>
            Seq(
              "io.github.sbt-doctest" %% "doctest-runtime" % DoctestBuildInfo.version % Test
            )
        }
      } else {
        Nil
      }
    },
    doctestScalafmt := true,
    doctestTestFramework := (doctestTestFramework ?? ScalaCheck).value,
    doctestScalaTestVersion := (doctestScalaTestVersion ?? None).value,
    doctestDecodeHtmlEntities := (doctestDecodeHtmlEntities ?? false).value,
    doctestOnlyCodeBlocksMode := (doctestOnlyCodeBlocksMode ?? false).value,
    doctestMarkdownEnabled := (doctestMarkdownEnabled ?? false).value,
    doctestMarkdownPathFinder := baseDirectory.value * "*.md",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    doctestIgnoreRegex := None,
    doctestDialect := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          DoctestDialect.Scala212Source3
        case "2.13" =>
          DoctestDialect.Scala213Source3
        case "3" =>
          DoctestDialect.Scala3
        case _ =>
          DoctestDialect.Scala213Source3
      }
    },
    doctestGenTests := Def.uncached {
      (Test / managedSourceDirectories).value.headOption match {
        case None =>
          streams.value.log.warn("DocTest: `Test/managedSourceDirectories` is empty. Failed to generate tests")
          Seq.empty
        case Some(testDir) =>
          val scalaTestVersion = doctestScalaTestVersion.value
            .orElse(
              TestGenResolver.findScalaTestVersionFromScalaBinaryVersion(
                managedJars(Test, classpathTypes.value, update.value, fileConverter.value),
                scalaBinaryVersion.value
              )
            )
          val testGen = TestGenResolver.resolve(doctestTestFramework.value, scalaTestVersion)

          val sourceFiles = (Compile / unmanagedSources).value ++ (Compile / managedSources).value
          val log = streams.value.log
          log.debug(s"DocTest: Applying ignore pattern [${doctestIgnoreRegex.value}] to exclude matching sources...")
          val filteredSourceFiles =
            doctestIgnoreRegex.value.fold(sourceFiles) { regex =>
              val IgnoreRegex = regex.r
              sourceFiles.filterNot { f =>
                FilenameUtils.normalize(f.getCanonicalPath, true) match {
                  case ign @ IgnoreRegex(_*) =>
                    log.debug(s"DocTest: Excluding source file: $ign")
                    true
                  case use =>
                    log.debug(s"DocTest: Using source file: $use")
                    false
                }
              }
            }

          val localBase = (LocalRootProject / baseDirectory).value

          val input = Input(
            localBase.getCanonicalPath,
            filteredSourceFiles
              .filter(_.ext == "scala")
              .flatMap(
                IO.relativize(localBase, _)
              ),
            findEncoding((Compile / scalacOptions).value).getOrElse("UTF-8"),
            testGen,
            doctestDecodeHtmlEntities.value,
            doctestOnlyCodeBlocksMode.value,
            doctestDialect.value,
            if (doctestMarkdownEnabled.value) {
              doctestMarkdownPathFinder.value
                .filter(!_.isDirectory)
                .get()
                .sortBy(_.getCanonicalPath)
                .zipWithIndex
            } else {
              Nil
            },
            baseDirectory.value.getCanonicalPath,
            if (doctestScalafmt.value) {
              // https://github.com/scalameta/sbt-scalafmt/blob/e59fc02237374e6/plugin/src/main/scala/org/scalafmt/sbt/ScalafmtPlugin.scala#L42-L45
              TaskKey[File]("scalafmtConfig").?.value.filter(_.isFile).map(IO.read(_))
            } else {
              None
            },
            testDir
          )
          val res = generateCode(
            "sbt-doctest-gen",
            input,
            sbtLauncher(doctestGenTests).value,
            (doctestGenTests / forkOptions).value,
            Nil
          )
          val result = res.decodeFromJsonString[Output]
          result.files.map(file)
      }
    },
    doctestGenTests := {
      if (supportScalaBinaryVersions(scalaBinaryVersion.value)) {
        doctestGenTests.value
      } else {
        streams.value.log.warn(s"sbt-doctest does not support Scala ${scalaVersion.value}")
        Nil
      }
    },
    Test / sourceGenerators += doctestGenTests.taskValue,
    watchSources ++= Def.uncached {
      val pathFinder = doctestMarkdownPathFinder.value
      if (doctestMarkdownEnabled.value) {
        pathFinder.get().map(new Source(_, AllPassFilter, NothingFilter))
      } else {
        Seq.empty
      }
    }
  )

  val doctestSettings = doctestGenSettings

  private def sbtLauncher[A](k: TaskKey[A]): Def.Initialize[Task[File]] = Def.taskDyn {
    val v = (k / sbtVersion).value
    Def.task {
      val Seq(launcher) = getJarFiles("org.scala-sbt" % "sbt-launch" % v).value
      launcher
    }
  }

  private def getJarFiles(module: ModuleID): Def.Initialize[Task[Seq[File]]] = Def.task {
    dependencyResolution.value
      .retrieve(
        dependencyId = module,
        scalaModuleInfo = scalaModuleInfo.value,
        retrieveDirectory = csrCacheDirectory.value,
        log = streams.value.log
      )
      .left
      .map(e => throw e.resolveException)
      .merge
      .distinct
  }

  private implicit class JsonStringOps(private val string: String) extends AnyVal {
    def decodeFromJsonString[A](implicit r: sjsonnew.JsonReader[A]): A = {
      val json = sjsonnew.support.scalajson.unsafe.Parser.parseUnsafe(string)
      val unbuilder = new sjsonnew.Unbuilder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      r.read(Some(json), unbuilder)
    }
  }

  private implicit class JsonOps[A](private val self: A) extends AnyVal {
    def toJsonString(implicit w: sjsonnew.JsonWriter[A]): String = {
      val builder = new sjsonnew.Builder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      w.write(self, builder)
      CompactPrinter.apply(
        builder.result.getOrElse(sys.error("invalid json"))
      )
    }
  }
  private def generateCode(
      projectName: String,
      input: Input,
      launcher: File,
      forkOptions: ForkOptions,
      extraSettings: Seq[String]
  ): String = {
    val buildSbt =
      s"""|//autoScalaLibrary := false
          |scalaVersion := "2.13.16"
          |Compile / sources := Nil
          |name := "${projectName}"
          |libraryDependencies := Seq("io.github.sbt-doctest" % "doctest-generator_2.13" % "${DoctestBuildInfo.version}")
          |${extraSettings.mkString("\n\n")}
          |""".stripMargin

    IO.withTemporaryDirectory { dir =>
      val forkOpt = forkOptions.withWorkingDirectory(dir)
      val out = dir / "out.json"
      val in = dir / "in.json"
      IO.write(dir / "build.sbt", buildSbt.getBytes(StandardCharsets.UTF_8))
      IO.write(in, input.toJsonString.getBytes(StandardCharsets.UTF_8))
      val ret = Fork.java.apply(
        forkOpt,
        Seq(
          "-jar",
          launcher.getCanonicalPath,
          Seq(
            "runMain",
            "com.github.tkawachi.doctest.GeneratorMain",
            s"--input=${in.getCanonicalPath}",
            s"--output=${out.getCanonicalPath}"
          ).mkString(" ")
        )
      )
      if (ret == 0) {
        IO.read(out)
      } else {
        sys.error(s"failure ${ret}")
      }
    }
  }
}
