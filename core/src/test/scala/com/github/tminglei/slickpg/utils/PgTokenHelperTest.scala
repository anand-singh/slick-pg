package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._

class PgTokenHelperTest {
  import PgTokenHelper._

  @Test
  def testTokenize(): Unit = {
    val input = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\"\"\"\"2010-01-01 14:30:00\"\"\"\",\"\"\"\"2010-01-03 15:30:00\"\"\"\")\"\")\",t)"}"""
    val tokens = Tokenizer.tokenize(input)

    val expected = Open("{") +: Open("(","\"") +: Chunk("201") +: Comma +: Open("(","\\\"") +: Chunk("101") +: Comma +: Open("(","\\\"\\\"") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Open("[","\\\"\\\"") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")","\\\"\\\"") +: Close(")","\\\"") +: Comma +: Chunk("t") +: Close(")","\"") +: Close("}") +: Nil

    assertEquals(expected, tokens)
  }

  @Test
  def testGrouping(): Unit = {
    val tokens = Open("{") +: Open("(","\"") +: Chunk("201") +: Comma +: Open("(","\\\"") +: Chunk("101") +: Comma +: Open("(","\\\"\\\"") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Open("[","\\\"\\\"") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")","\\\"\\\"") +: Close(")","\\\"") +: Comma +: Chunk("t") +: Close(")","\"") +: Close("}") +: Nil
    val root = grouping(tokens)

    val expected =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(","\""),
          Chunk("201"),
          Comma,
          GroupToken(List(
            Open("(","\\\""),
            Chunk("101"),
            Comma,
            GroupToken(List(
              Open("(","\\\"\\\""),
              Chunk("test1'"),
              Marker("\\\"\\\"")
            )),
            Comma,
            GroupToken(List(
              Marker("\\\"\\\""),
              Chunk("2001-01-03 13:21:00"),
              Marker("\\\"\\\"")
            )),
            Comma,
            GroupToken(List(
              Open("[","\\\"\\\""),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-01 14:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Comma,
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-03 15:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Close(")","\\\"\\\"")
            )),
            Close(")","\\\"")
          )),
          Comma,
          Chunk("t"),
          Close(")","\"")
        )),
        Close("}")
      ))

    assertEquals(expected, root)

    val root1 = grouping(Tokenizer.tokenize("""{"(\"(test1'\",,,)"}"""))
    val expected1 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          GroupToken(List(
            Open("(", marker = "\\\""),
            Chunk("test1'"),
            Marker("\\\"")
          )),
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assertEquals(expected1, root1)

    val root2 = grouping(Tokenizer.tokenize(("""{"(,102,,)"}""")))
    val expected2 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          Null,
          Comma,
          Chunk("102"),
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assertEquals(expected2, root2)

    val root3 = grouping(Tokenizer.tokenize("""{"(,,,)"}"""))
    val expected3 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assertEquals(expected3, root3)
  }

  @Test
  def testGetString(): Unit = {
    val input =
      GroupToken(List(
        Open("[","\\\"\\\""),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Comma,
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Close(")","\\\"\\\"")
      ))
    val rangeStr = getString(input, 2)

    val expected = """["2010-01-01 14:30:00","2010-01-03 15:30:00")"""
    assertEquals(expected, rangeStr)
  }

  @Test
  def testGetChildren(): Unit = {
    val input =
      GroupToken(List(
        Open("[","\\\"\\\""),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Comma,
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Close(")","\\\"\\\"")
      ))
    val children = getChildren(input)

    val expected =
      List(
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        ))
      )

    assertEquals(expected, children)
  }

  @Test
  def testCreateString(): Unit = {
    val input =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(","\""),
          Chunk("201"),
          GroupToken(List(
            Open("(","\\\""),
            Chunk("101"),
            GroupToken(List(
              Open("(","\\\"\\\""),
              Chunk("test1'"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\""),
              Chunk("2001-01-03 13:21:00"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Open("[","\\\"\\\""),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-01 14:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-03 15:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Close(")","\\\"\\\"")
            )),
            Close(")","\\\"")
          )),
          Chunk("t"),
          Close(")","\"")
        )),
        Close("}")
      ))
    val pgStr = createString(input)

    val expected = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\\\\\"\"2010-01-01 14:30:00\\\\\"\",\\\\\"\"2010-01-03 15:30:00\\\\\"\")\"\")\",t)"}"""

    assertEquals(expected, pgStr)

    ///
    val input1 =
      GroupToken(List(
        Open("(","\""),
        Chunk("201"),
        GroupToken(List(
          Open("(","\\\""),
          Chunk("101"),
          GroupToken(List(
            Open("(","\\\"\\\""),
            Chunk("test1'"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Marker("\\\"\\\""),
            Chunk("2001-01-03 13:21:00"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Open("[","\\\"\\\""),
            GroupToken(List(
              Marker("\\\"\\\"\\\"\\\""),
              Chunk("2010-01-01 14:30:00"),
              Marker("\\\"\\\"\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\"\\\"\\\""),
              Chunk("2010-01-03 15:30:00"),
              Marker("\\\"\\\"\\\"\\\"")
            )),
            Close(")","\\\"\\\"")
          )),
          Close(")","\\\"")
        )),
        Chunk("t"),
        Close(")","\"")
      ))
    val pgStr1 = createString(input1)

    val expected1 = """(201,"(101,""(test1'"",""2001-01-03 13:21:00"",""[\\""2010-01-01 14:30:00\\"",\\""2010-01-03 15:30:00\\"")"")",t)"""
    assertEquals(expected1, pgStr1)
  }

}