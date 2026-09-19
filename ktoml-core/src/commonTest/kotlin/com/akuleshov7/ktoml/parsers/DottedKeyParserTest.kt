package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DottedKeyParserTest {
    @Test
    fun positiveParsingTest() {
        var test = TomlKey("\"a.b.c\"", 0)
        assertEquals("a.b.c", test.last())
        assertEquals(false, test.isDotted)

        test = TomlKey("\"a.b.c\".b.c", 0)
        assertEquals("c", test.last())
        assertEquals(listOf("\"a.b.c\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"a\".b.c", 0)
        assertEquals("c", test.last())
        assertEquals(listOf("\"a\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"  a  \"", 0)
        assertEquals("  a  ", test.last())
        assertEquals(false, test.isDotted)

        test = TomlKey("\"\\ttab\\ttab\\t\"", 0)
        assertEquals("\ttab\ttab\t", test.last())
        assertEquals(false, test.isDotted)

        test = TomlKey("a.b.c", 0)
        assertEquals("c", test.last())
        assertEquals(true, test.isDotted)

        test = TomlKey("a.\"  b  .c \"", 0)
        assertEquals("  b  .c ", test.last())
        assertEquals(true, test.isDotted)

        test = TomlKey("a  .  b .  c ", 0)
        assertEquals("c", test.last())
        assertEquals(true, test.isDotted)

        assertFailsWith<ParseException> { TomlKey("SPACE AND SPACE", 0) }
    }

    @Test
    fun createTable() {
        var test = TomlKeyValuePrimitive(Pair("google.com","5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("google", test.fullTableKey.toString())

        test = TomlKeyValuePrimitive(Pair("a.b.c.d", "5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("a.b.c", test.fullTableKey.toString())

        val testKeyValue = TomlKeyValuePrimitive(Pair("a.b.c", "5"), 0)
        assertEquals("c", testKeyValue.key.last())
    }

    @Test
    fun parseDottedKey1() {
        val string = """
            ["a.b.c"]
            f.e.g."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable (["a.b.c"])
                |         - TomlTable (["a.b.c".f])
                |             - TomlTable (["a.b.c".f.e])
                |                 - TomlTable (["a.b.c".f.e.g])
                |                     - TomlTable (["a.b.c".f.e.g."a.b.c"])
                |                         - TomlKeyValuePrimitive (d=10)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parseDottedKey2() {
        val string = """
            a."a.b.c".d = 10
            ["a.b.c"]
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable ([a])
                 |         - TomlTable ([a."a.b.c"])
                 |             - TomlKeyValuePrimitive (d=10)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlStubEmptyNode (technical_node)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parseSimpleDottedKey() {
        val string = """
            ["a.b.c"]
                a."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlTable (["a.b.c".a])
                 |             - TomlTable (["a.b.c".a."a.b.c"])
                 |                 - TomlKeyValuePrimitive (d=10)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Serializable
    data class QuotedSegmentRoot(val a: QuotedSegmentMiddle)

    @Serializable
    data class QuotedSegmentMiddle(@SerialName("b.c") val bc: QuotedSegmentLeaf)

    @Serializable
    data class QuotedSegmentLeaf(val d: Long)

    @Serializable
    data class QuotedHeaderRoot(@SerialName("a.b.c") val abc: QuotedHeaderTable)

    @Serializable
    data class QuotedHeaderTable(val f: QuotedHeaderMiddle)

    @Serializable
    data class QuotedHeaderMiddle(@SerialName("g.h") val gh: QuotedHeaderLeaf)

    @Serializable
    data class QuotedHeaderLeaf(val i: Long)

    @Serializable
    data class SingleValue(val a: Long)

    @Serializable
    data class EofTable(val a: EofInner)

    @Serializable
    data class EofInner(val b: Long)

    @Test
    fun quotedDottedSegmentWithDotsInsideQuotes() {
        val string = """
            a."b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable ([a])
                 |         - TomlTable ([a."b.c"])
                 |             - TomlKeyValuePrimitive (d=10)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(
            QuotedSegmentRoot(QuotedSegmentMiddle(QuotedSegmentLeaf(10))),
            Toml.decodeFromString<QuotedSegmentRoot>(string)
        )
    }

    @Test
    fun quotedDottedSegmentUnderQuotedTableHeader() {
        val string = """
            ["a.b.c"]
            f."g.h".i = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlTable (["a.b.c".f])
                 |             - TomlTable (["a.b.c".f."g.h"])
                 |                 - TomlKeyValuePrimitive (i=10)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(
            QuotedHeaderRoot(QuotedHeaderTable(QuotedHeaderMiddle(QuotedHeaderLeaf(10)))),
            Toml.decodeFromString<QuotedHeaderRoot>(string)
        )
    }

    @Test
    fun emptyMiddleSegmentIsRejected() {
        val exception = assertFailsWith<TomlWritingException> {
            Toml.decodeFromString<Map<String, Map<String, Long>>>("a..b = 1")
        }
        // the rejected key path (with the empty segment) should be reported
        assertTrue(
            exception.message!!.contains("[a, ]"),
            "exception should contain the offending key path, but was: ${exception.message}"
        )
    }

    @Test
    fun trailingEmptySegmentIsRejected() {
        val exception = assertFailsWith<TomlWritingException> {
            Toml.decodeFromString<Map<String, Map<String, Long>>>("a. = 1")
        }
        assertTrue(
            exception.message!!.contains("empty key part"),
            "exception should mention the empty key part, but was: ${exception.message}"
        )
    }

    @Test
    fun duplicateKeyDecodeKeepsLastValue() {
        // ktoml currently does not reject duplicate keys: both nodes stay in the
        // tree and the decoder sees the last value - this pins that behavior
        val string = """
            a = 1
            a = 2
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlKeyValuePrimitive (a=1)
                 |     - TomlKeyValuePrimitive (a=2)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(SingleValue(2), Toml.decodeFromString<SingleValue>(string))
    }

    @Test
    fun bareKeyWithSpacesFailsWithLineNumberAndKey() {
        val exception = assertFailsWith<ParseException> {
            Toml.tomlParser.parseString(
                """
                    x = 1

                    a b = 2
                """.trimIndent()
            )
        }
        assertTrue(
            exception.message!!.startsWith("Line 3:"),
            "exception should point at line 3, but was: ${exception.message}"
        )
        assertTrue(
            exception.message!!.contains("[a b]"),
            "exception should contain the offending key, but was: ${exception.message}"
        )
    }

    @Test
    fun unclosedQuoteInKeyFailsWithLineNumber() {
        val exception = assertFailsWith<ParseException> {
            Toml.tomlParser.parseString(
                """
                    x = 1
                    "a.b = 2
                """.trimIndent()
            )
        }
        assertTrue(
            exception.message!!.startsWith("Line 2:"),
            "exception should point at line 2, but was: ${exception.message}"
        )
        assertTrue(
            exception.message!!.contains("\"a.b"),
            "exception should contain the offending key, but was: ${exception.message}"
        )
    }

    @Test
    fun endOfFileWithoutTrailingNewline() {
        // no trailing newline at the end of the input
        val string = "[a]\nb = 1"

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable ([a])
                 |         - TomlKeyValuePrimitive (b=1)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(EofTable(EofInner(1)), Toml.decodeFromString<EofTable>(string))
    }
}
