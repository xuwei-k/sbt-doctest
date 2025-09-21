package com.github.tkawachi.doctest

import com.github.tkawachi.doctest.DoctestPlugin.DoctestTestFramework
import com.github.tkawachi.doctest.DoctestPlugin.DoctestTestFramework.*
import sbt.Keys.Classpath
import sbt.internal.util.MessageOnlyException
import sbt.librarymanagement.CrossVersion
import sbt.librarymanagement.ModuleID
import scala.util.Try

object TestGenResolver {
  def resolve(framework: DoctestTestFramework, scalaTestVersion: Option[String]): TestGenType = {
    framework match {
      case MicroTest => TestGenType.MicroTest
      case Minitest => TestGenType.Minitest
      case ScalaTest =>
        scalaTestVersion
          .flatMap {
            _.split('.').take(2).flatMap(s => Try(s.toInt).toOption) match {
              case Array(major, minor) =>
                if (major < 3 || major == 3 && minor == 0) {
                  Some(TestGenType.ScalaTest30)
                } else {
                  Some(TestGenType.ScalaTest31)
                }
              case _ =>
                None
            }
          }
          .getOrElse(throw new MessageOnlyException(s"Unexpected doctestScalaTestVersion version: $scalaTestVersion"))
      case Specs2 => TestGenType.Specs2
      case ScalaCheck => TestGenType.ScalaCheck
      case Munit => TestGenType.Munit
    }
  }

  @deprecated(message = "use findScalaTestVersionFromScalaBinaryVersion", since = "0.9.8")
  def findScalaTestVersion(testClasspath: Classpath, scalaVersion: String): Option[String] = {
    val scalaBinaryVersion = CrossVersion.binaryScalaVersion(scalaVersion)
    findScalaTestVersionFromScalaBinaryVersion(testClasspath, scalaBinaryVersion)
  }

  def findScalaTestVersionFromScalaBinaryVersion(
      testClasspath: Classpath,
      scalaBinaryVersion: String
  ): Option[String] =
    DoctestPlugin.findScalaTestVersionFromScalaBinaryVersion(
      testClasspath = testClasspath,
      scalaBinaryVersion = scalaBinaryVersion
    )

  private[doctest] def detectScalaTestVersion(moduleID: ModuleID, scalaBinVersion: String): Option[String] = {
    // From ScalaTest 3.2, scalatest-core is used.
    if (
      moduleID.organization == "org.scalatest" &&
      (moduleID.name == s"scalatest_$scalaBinVersion" || moduleID.name == s"scalatest-core_$scalaBinVersion")
    )
      Some(moduleID.revision)
    else None
  }
}
