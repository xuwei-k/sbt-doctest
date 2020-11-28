import java.nio.charset.StandardCharsets

import complete.DefaultParsers._

crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.4")

javacOptions ++= (scalaVersion.value match {
  case v if v startsWith "2.13." => Seq("-source", "1.8", "-target", "1.8")
  case v if v startsWith "2.12." => Seq("-source", "1.8", "-target", "1.8")
  case v if v startsWith "2.11." => Seq("-source", "1.8", "-target", "1.6")
})

scalacOptions         := Seq("-Ywarn-dead-code")
scalacOptions in Test -= "-Ywarn-dead-code"
scalacOptions        ++= (scalaVersion.value match {
  case v if v startsWith "2.13." => Seq("-target:jvm-1.8")
  case v if v startsWith "2.12." => Seq("-target:jvm-1.8", "-opt:l:method")
  case v if v startsWith "2.11." => Seq("-target:jvm-1.6")
})

// Declares scalatest, scalacheck, minitest and utest dependencies explicitly.
libraryDependencies ++= Seq(
  "com.lihaoyi"    %% "utest"             % "0.6.4"  % Test,
  "org.scalatest"  %% "scalatest"         % "3.0.9"  % Test,
  "org.scalacheck" %% "scalacheck"        % "1.14.0" % Test,
  "io.monix"       %% "minitest"          % "2.8.2"  % Test,
  "io.monix"       %% "minitest-laws"     % "2.8.2"  % Test,
  "org.specs2"     %% "specs2-core"       % "4.3.6"  % Test,
  "org.specs2"     %% "specs2-scalacheck" % "4.3.6"  % Test
)

doctestMarkdownEnabled    := true
doctestMarkdownPathFinder := baseDirectory.value ** "*.md"
doctestIgnoreRegex        := Some("(.*)IgnoreMe.scala")

testFrameworks += new TestFramework("minitest.runner.Framework")

//ignore Failing
Test / testOptions := Seq(Tests.Filter(s => !s.contains("Failing")))

val existsInFile = inputKey[Unit]("Ensure a given string exists in a file")

existsInFile := {
  val Seq(searchKey, fileName) = spaceDelimited("<arg>").parsed
  val fileContent = IO.read(file(fileName), StandardCharsets.UTF_8)
  if (!fileContent.contains(searchKey)) {
    sys.error(s"$searchKey doesn't exist in $fileName")
  }
}
