package com.github.tkawachi.doctest

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Positional

trait GenericParser extends RegexParsers {
  override def skipWhitespace = false

  case class Prompt(head: String, tail: String)

  case class PositionedString(str: String) extends Positional

  def append(s: String ~ String): String = s._1 + s._2

  val horizontalWhiteSpace: Parser[String] = "[ \\t]+".r

  val emptyStr: Parser[String] = ""

  val anyStr: Parser[String] = "[^\\r\\n]*".r

  val anyStr1: Parser[String] = "[^\\r\\n]+".r

  val anyPosStr1: Parser[PositionedString] = positioned(anyStr1 ^^ PositionedString)

  val lineSep = System.lineSeparator()

  val eol = '\r'.? ~> '\n'

  val leadingString = {
    val leadingChar = elem('/') | '*' | ' ' | '\t'
    leadingChar.* ^^ (_.mkString)
  }

  def headPromptLine(prompt: String, begin: Parser[PositionedString]): Parser[String ~ PositionedString] =
    (leadingString <~ prompt) ~ (begin <~ eol)

  def tailPromptLine(leading: String, prompt: String): Parser[String] =
    leading ~> prompt ~> anyStr <~ eol

  def multiLine(prompt: Prompt, begin: Parser[PositionedString]): Parser[(String, PositionedString)] =
    headPromptLine(prompt.head, begin) >> {
      case leading ~ first =>
        tailPromptLine(leading, prompt.tail).* ^^ {
          case rest =>
            val code = (first.str :: rest).mkString(lineSep)
            val posCode = PositionedString(code)
            posCode.pos = first.pos
            (leading, posCode)
        }
    }

  private val verbatimBegin = {
    val keywords = "abstract" | "case" | "class" | "def" | "implicit" | "import" |
      "lazy" | "object" | "sealed" | "trait" | "type" | "val" | "var"
    val whiteSpacePlusAnyStr = horizontalWhiteSpace ~ anyStr ^^ append
    keywords ~ (whiteSpacePlusAnyStr | emptyStr) ^^ PositionedString.compose(append)
  }

  def verbatim(prompt: Prompt): Parser[Verbatim] =
    multiLine(prompt, verbatimBegin) ^^ { case (_, code) => Verbatim(code.str) }

  def example(prompt: Prompt, resultLine: String => Parser[TestResult]): Parser[Example] =
    multiLine(prompt, anyPosStr1) >> {
      case (leading, expr) =>
        resultLine(leading) ^^ (Example(expr.str, _, expr.pos.line))
    }
}

trait PythonStyleParser extends GenericParser {
  val pyPrompt = Prompt(
    ">>> ",
    "... ")

  def pyResultLine(leading: String) = leading ~> anyStr1 <~ eol ^^ (TestResult(_))

  val pyComponents = verbatim(pyPrompt) | example(pyPrompt, pyResultLine)
}

trait ReplStyleParser extends GenericParser {
  val replPrompt = Prompt(
    "scala> ",
    "     | ")

  private val res: Parser[String] = "res\\d+".r
  private val tpe: Parser[String] = "((?! = )[^\\n\\r])+".r

  def replResultLine(leading: String) = {
    (leading ~> res ~> ": " ~> tpe <~ " = ") ~ (anyStr1 <~ eol) ^^ {
      case parsedType ~ value => TestResult(value, Some(parsedType.trim))
    }
  }

  val replComponents = verbatim(replPrompt) | example(replPrompt, replResultLine)
}

trait PropertyStyleParser extends GenericParser {
  val propPrompt = Prompt(
    "prop> ",
    "    | ")

  val property = multiLine(propPrompt, anyPosStr1) ^^ {
    case (_, expr) => Property(expr.str, expr.pos.line)
  }

  val propComponents = verbatim(propPrompt) | property
}

object CommentParser extends PythonStyleParser with ReplStyleParser with PropertyStyleParser {
  val anyLine = anyStr <~ eol

  val components = pyComponents | replComponents | propComponents

  val allLines = (components ^^ Some.apply | anyLine ^^^ None).* <~ anyStr ^^ (_.flatten)

  def parse(input: String) = parseAll(allLines, input)

  def apply(comment: ScaladocComment): Either[String, ParsedDoctest] =
    parse(comment.text) match {
      case Success(examples, _) =>
        Right(ParsedDoctest(comment.pkg, comment.symbol, examples, comment.lineNo, Nil))

      case NoSuccess(msg, next) =>
        Left(s"$msg on line ${next.pos.line}, column ${next.pos.column}")
    }
}
