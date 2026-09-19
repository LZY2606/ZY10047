package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString

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
}

/**
 * Combined coverage for dotted keys interacting with quoting, comments,
 * multiline strings, escapes and duplicate definitions.
 */
class DottedKeyParserTestExtended {
    @kotlinx.serialization.Serializable
    data class QuotedRoot(val a: QuotedA)

    @kotlinx.serialization.Serializable
    data class QuotedA(@kotlinx.serialization.SerialName("b.c") val bc: QuotedBc)

    @kotlinx.serialization.Serializable
    data class QuotedBc(val d: Long)

    @kotlinx.serialization.Serializable
    data class SingleValue(val a: Long)

    @kotlinx.serialization.Serializable
    data class TwoValues(val a: Long, val b: Long)

    @kotlinx.serialization.Serializable
    data class DottedString(val a: DottedStringInner)

    @kotlinx.serialization.Serializable
    data class DottedStringInner(val b: String)

    @kotlinx.serialization.Serializable
    data class SingleString(val s: String)

    @Test
    fun quotedDottedSegmentDefinesOwnership() {
        val parsedToml = Toml.tomlParser.parseString("a.\"b.c\".d = 1")
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([a])
                |         - TomlTable ([a."b.c"])
                |             - TomlKeyValuePrimitive (d=1)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            QuotedRoot(QuotedA(QuotedBc(1))),
            Toml.decodeFromString<QuotedRoot>("a.\"b.c\".d = 1")
        )
    }

    @Test
    fun literalQuotedDottedSegmentDefinesOwnership() {
        val parsedToml = Toml.tomlParser.parseString("a.'b.c'.d = 2")
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([a])
                |         - TomlTable ([a.'b.c'])
                |             - TomlKeyValuePrimitive (d=2)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            QuotedRoot(QuotedA(QuotedBc(2))),
            Toml.decodeFromString<QuotedRoot>("a.'b.c'.d = 2")
        )
    }

    @Test
    fun emptySegmentIsKeptByParserButRejectedOnWrite() {
        // the parser preserves the empty segment in the key parts ...
        assertEquals(listOf("a", "", "b"), TomlKey("a..b", 1).keyParts)
        // ... and emitting such a key is rejected, naming the broken key parts
        val exception = assertFailsWith<com.akuleshov7.ktoml.exceptions.TomlWritingException> {
            TomlKey("a..b", 1).toString()
        }
        assertTrue(exception.message!!.contains("empty"), "message should mention the empty key part: ${exception.message}")
        assertTrue(exception.message!!.contains("a"), "message should contain the key path: ${exception.message}")
        assertTrue(exception.message!!.contains("b"), "message should contain the key path: ${exception.message}")
        // a parsed file with an empty segment fails the same way when its tree is rendered
        val parsedToml = Toml.tomlParser.parseString("a..b = 1")
        assertEquals(1, parsedToml.children.size)
        assertFailsWith<com.akuleshov7.ktoml.exceptions.TomlWritingException> {
            parsedToml.prettyStr()
        }
    }

    @Test
    fun unclosedQuoteInDottedKeyFailsWithLineNumber() {
        val exception = assertFailsWith<ParseException> {
            Toml.tomlParser.parseString("ok = 1\nstill_ok = 2\nbroken.\"unclosed = 3")
        }
        assertTrue(exception.message!!.contains("Line 3"), "message should contain the line number: ${exception.message}")
        assertTrue(exception.message!!.contains("broken.\"unclosed"), "message should contain the key: ${exception.message}")
    }

    @Test
    fun bareKeyWithSpacesFailsWithLineNumberAndKey() {
        val exception = assertFailsWith<ParseException> {
            Toml.tomlParser.parseString("ok = 1\nBAD KEY = 2")
        }
        assertTrue(exception.message!!.contains("Line 2"), "message should contain the line number: ${exception.message}")
        assertTrue(exception.message!!.contains("BAD KEY"), "message should contain the key: ${exception.message}")
    }

    @Test
    fun duplicateKeysAreKeptInTreeAndLastOneWinsOnDecode() {
        val parsedToml = Toml.tomlParser.parseString("a = 1\na = 2")
        assertEquals(2, parsedToml.children.size)
        val first = parsedToml.children[0] as TomlKeyValuePrimitive
        val second = parsedToml.children[1] as TomlKeyValuePrimitive
        assertEquals(1, first.lineNo)
        assertEquals(2, second.lineNo)
        assertEquals("a", first.key.last())
        assertEquals("a", second.key.last())
        assertEquals(SingleValue(2), Toml.decodeFromString<SingleValue>("a = 1\na = 2"))
    }

    @Test
    fun commentsGluedToKeyValuePairs() {
        val parsedToml = Toml.tomlParser.parseString("# first\n# second\na = 1 # trailing\nb = 2")
        assertEquals(2, parsedToml.children.size)
        val a = parsedToml.children[0] as TomlKeyValuePrimitive
        assertEquals(listOf("first", "second"), a.comments)
        assertEquals("trailing", a.inlineComment)
        val b = parsedToml.children[1] as TomlKeyValuePrimitive
        assertEquals(emptyList(), b.comments)
        assertEquals("", b.inlineComment)
        assertEquals(TwoValues(1, 2), Toml.decodeFromString<TwoValues>("# first\n# second\na = 1 # trailing\nb = 2"))
    }

    @Test
    fun multilineBasicStringUnderDottedKey() {
        val input = "a.b = \"\"\"first\nsecond\"\"\""
        val parsedToml = Toml.tomlParser.parseString(input)
        val tableA = parsedToml.children.single() as com.akuleshov7.ktoml.tree.nodes.TomlTable
        assertEquals("a", tableA.fullTableKey.toString())
        val keyValue = tableA.children.single() as TomlKeyValuePrimitive
        assertEquals("b", keyValue.key.last())
        assertEquals("first\nsecond", keyValue.value.content)
        assertEquals(DottedString(DottedStringInner("first\nsecond")), Toml.decodeFromString<DottedString>(input))
    }

    @Test
    fun multilineLiteralStringKeepsNewlinesVerbatim() {
        val input = "s = '''one\ntwo'''"
        val parsedToml = Toml.tomlParser.parseString(input)
        val keyValue = parsedToml.children.single() as TomlKeyValuePrimitive
        assertEquals("one\ntwo", keyValue.value.content)
        assertEquals(SingleString("one\ntwo"), Toml.decodeFromString<SingleString>(input))
    }

    @Test
    fun lineEndingBackslashConsumesNewlineAndLeadingWhitespace() {
        val input = "s = \"\"\"one \\\n    two\"\"\""
        val parsedToml = Toml.tomlParser.parseString(input)
        val keyValue = parsedToml.children.single() as TomlKeyValuePrimitive
        assertEquals("one two", keyValue.value.content)
        assertEquals(SingleString("one two"), Toml.decodeFromString<SingleString>(input))
    }

    @Test
    fun unicodeSurrogatePairEscapeIsCombined() {
        val input = "s = \"\\uD83D\\uDE00\""
        val parsedToml = Toml.tomlParser.parseString(input)
        val keyValue = parsedToml.children.single() as TomlKeyValuePrimitive
        assertEquals("\uD83D\uDE00", keyValue.value.content)
        assertEquals(SingleString("\uD83D\uDE00"), Toml.decodeFromString<SingleString>(input))
    }

    @Test
    fun endOfFileWithoutTrailingNewline() {
        val parsedToml = Toml.tomlParser.parseString("a = 1")
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlKeyValuePrimitive (a=1)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(SingleValue(1), Toml.decodeFromString<SingleValue>("a = 1"))
    }
}
