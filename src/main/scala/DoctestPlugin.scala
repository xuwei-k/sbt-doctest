import com.github.tkawachi.doctest.{ TestFramework => TFramework, Specs2, ScalaTest, TestGenerator }
import sbt._, Keys._
import xsbti.Position

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
object DoctestPlugin extends Plugin {
  val doctestTestFramework = settingKey[String]("Test framework. Specify scalatest (default) or specs2.")
  val doctestWithDependencies = settingKey[Boolean]("Whether to include libraryDependencies to doctestSettings.")
  val doctestGenTests = taskKey[(Seq[File], Seq[TestGenerator.Result])]("Generates test files.")
  val enableSourcePositionMapper = settingKey[Boolean]("enable sourcePositionMapper")

  /**
   * Default libraryDependencies.
   */
  object TestLibs {
    val scalatest: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
    )

    val specs2Version = "2.4.4"

    val specs2 = Seq(
      "org.specs2" %% "specs2-core" % specs2Version % "test",
      "org.specs2" %% "specs2-scalacheck" % specs2Version % "test"
    )
  }

  /**
   * Settings for test Generation.
   */
  val doctestGenSettings = Seq(
    doctestTestFramework := "scalatest",
    doctestWithDependencies := true,
    doctestGenTests := {
      (managedSourceDirectories in Test).value.headOption match {
        case None =>
          streams.value.log.warn("managedSourceDirectories in Test is empty. Failed to generate tests")
          Nil -> Nil
        case Some(testDir) =>
          (unmanagedSources in Compile).value
            .filter(_.ext == "scala")
            .flatMap(TestGenerator(_, TFramework(doctestTestFramework.value)))
            .groupBy(r => r.pkg -> r.basename)
            .map {
              case ((pkg, basename), results) =>
                results.zipWithIndex.map {
                  case (result, idx) =>
                    val writeBasename = if (idx == 0) basename else basename + idx
                    val writeDir = pkg.fold(testDir)(_.split("\\.").foldLeft(testDir) { (a: File, e: String) => new File(a, e) })
                    val writeFile = new File(writeDir, writeBasename + "Doctest.scala")
                    IO.write(writeFile, result.testSource)
                    writeFile
                } -> results
            }.foldLeft((Seq.empty[File], Seq.empty[TestGenerator.Result])) {
              case ((a, b), (c, d)) => (a ++ c) -> (b ++ d)
            }
      }
    },
    sourcePositionMappers ++= {
      if(enableSourcePositionMapper.value){
        doctestGenTests.map{
          _._2.flatMap{ _.positionMappers }
        }
      }else{
        Nil: Seq[Position => Option[Position]]
      }
    },
    sourceGenerators in Test += doctestGenTests.taskValue.map(_._1)
  )

  val doctestSettings = doctestGenSettings ++ Seq(
    libraryDependencies ++= (if (doctestWithDependencies.value) {
      TFramework(doctestTestFramework.value) match {
        case ScalaTest => TestLibs.scalatest
        case Specs2 => TestLibs.specs2
      }
    } else {
      Seq.empty
    })
  )
}
