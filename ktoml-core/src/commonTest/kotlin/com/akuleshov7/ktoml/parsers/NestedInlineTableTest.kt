package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedInlineTableTest {
    @Serializable
    data class InlineRoot(val t: InlineTable)

    @Serializable
    data class InlineTable(val b: List<Long>, val c: InlineNested)

    @Serializable
    data class InlineNested(val d: String)

    @Serializable
    data class DottedInlineRoot(val t: DottedInlineTable)

    @Serializable
    data class DottedInlineTable(val a: DottedInlineInner)

    @Serializable
    data class DottedInlineInner(val b: Long)

    @Serializable
    data class Text(val s: String)

    @Serializable
    data class AccentedKey(@SerialName("é") val e: String)

    @Serializable
    data class EmojiKey(@SerialName("\uD83D\uDE00") val emoji: Long)

    @Serializable
    data class GluedComments(val a: Long, val b: Long)

    private val tripleQuotes = "\"\"\""

    @Test
    fun inlineTableWithNestedArray() {
        val string = """
            t = { b = [1, 2], c = { d = "x" } }
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([t])
                |         - TomlTable ([t.c])
                |             - TomlKeyValuePrimitive (d="x")
                |         - TomlKeyValueArray (b=[ 1, 2 ])
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(
            InlineRoot(InlineTable(listOf(1, 2), InlineNested("x"))),
            Toml.decodeFromString<InlineRoot>(string)
        )
    }

    @Test
    fun inlineTableWithDottedKey() {
        val string = """
            t = { a.b = 7 }
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([t])
                |         - TomlTable ([t.a])
                |             - TomlKeyValuePrimitive (b=7)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(
            DottedInlineRoot(DottedInlineTable(DottedInlineInner(7))),
            Toml.decodeFromString<DottedInlineRoot>(string)
        )
    }

    @Test
    fun multilineBasicStringWithLineEndingBackslash() {
        // a backslash at the end of the line trims the newline and all
        // leading whitespace of the next line
        val string = "s = ${tripleQuotes}abc \\\n    def$tripleQuotes"

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlKeyValuePrimitive (s="abc def")
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(Text("abc def"), Toml.decodeFromString<Text>(string))
    }

    @Test
    fun multilineLiteralStringPreservesContent() {
        // the newline right after the opening delimiter is trimmed, the rest
        // of the content (including backslashes) is kept as is
        val string = "s = '''\nline1\nline2\\raw'''"

        assertEquals(
            Text("line1\nline2\\raw"),
            Toml.decodeFromString<Text>(string)
        )
    }

    @Test
    fun multilineBasicStringWithEscapedChars() {
        val string = "s = $tripleQuotes\na\\tb\\u00E9\\U0001F600$tripleQuotes"

        assertEquals(
            Text("a\tb\u00E9\uD83D\uDE00"),
            Toml.decodeFromString<Text>(string)
        )
    }

    @Test
    fun basicStringWithUnicodeSurrogatePairEscape() {
        // 😀 is outside of the BMP and is represented as a surrogate pair
        val string = "s = \"\\U0001F600\""

        assertEquals(
            Text("\uD83D\uDE00"),
            Toml.decodeFromString<Text>(string)
        )
    }

    @Test
    fun unicodeEscapeInQuotedKeyAndValue() {
        val string = "\"\\u00E9\" = \"caf\\u00E9\""

        assertEquals(
            AccentedKey("caf\u00E9"),
            Toml.decodeFromString<AccentedKey>(string)
        )
    }

    @Test
    fun emojiSurrogatePairInQuotedKey() {
        val string = "\"\uD83D\uDE00\" = 5"

        assertEquals(
            EmojiKey(5),
            Toml.decodeFromString<EmojiKey>(string)
        )
    }

    @Test
    fun commentsGluedToValuesAndKeys() {
        val string = """
            # leading comment
            a = 1 # sticky comment
            # middle comment
            b = 2# glued comment
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlKeyValuePrimitive (a=1)
                |     - TomlKeyValuePrimitive (b=2)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )

        assertEquals(
            GluedComments(1, 2),
            Toml.decodeFromString<GluedComments>(string)
        )
    }

    @Test
    fun multilineStringAtEofWithoutTrailingNewline() {
        // no trailing newline after the closing delimiter
        val string = "s = ${tripleQuotes}abc$tripleQuotes"

        assertEquals(Text("abc"), Toml.decodeFromString<Text>(string))
    }

    // ============ encode -> decode -> encode round trips ============
    // ktoml promises to encode properties in declaration order, so the
    // canonical text of the second encoding must match the first one
    // byte-for-byte; no re-sorting is applied in these assertions.

    @Serializable
    data class RoundTripPrimitives(val s: String, val l: Long, val d: Double, val b: Boolean)

    @Serializable
    data class RoundTripInner(val x: Long, val y: String)

    @Serializable
    data class RoundTripNested(val outer: RoundTripInner, val name: String)

    @Serializable
    data class RoundTripFruit(val name: String, val sku: Long)

    @Serializable
    data class RoundTripBasket(val fruits: List<RoundTripFruit>)

    @Serializable
    data class RoundTripLists(val tags: List<String>, val nums: List<Long>)

    @Serializable
    data class RoundTripDeepA(val b: RoundTripDeepB)

    @Serializable
    data class RoundTripDeepB(val c: String)

    @Serializable
    data class RoundTripDeep(val a: RoundTripDeepA)

    @Serializable
    data class RoundTripTexts(val multiline: String, val emoji: String)

    private inline fun <reified T> assertStableRoundTrip(value: T) {
        val firstEncoding = Toml.encodeToString(value)
        val decoded = Toml.decodeFromString<T>(firstEncoding)
        assertEquals(value, decoded, "decode(encode(value)) should return the original value")
        val secondEncoding = Toml.encodeToString(decoded)
        assertEquals(
            firstEncoding,
            secondEncoding,
            "the second encoding should produce the same canonical text"
        )
    }

    @Test
    fun roundTripPrimitives() {
        assertStableRoundTrip(RoundTripPrimitives("hi", 42L, 3.5, true))
    }

    @Test
    fun roundTripNestedTables() {
        assertStableRoundTrip(RoundTripNested(RoundTripInner(1L, "one"), "root"))
    }

    @Test
    fun roundTripArrayOfTables() {
        assertStableRoundTrip(
            RoundTripBasket(
                listOf(
                    RoundTripFruit("apple", 1L),
                    RoundTripFruit("banana", 2L)
                )
            )
        )
    }

    @Test
    fun roundTripPrimitiveArrays() {
        assertStableRoundTrip(RoundTripLists(listOf("a", "b"), listOf(1L, 2L, 3L)))
    }

    @Test
    fun roundTripDeeplyNestedTables() {
        assertStableRoundTrip(RoundTripDeep(RoundTripDeepA(RoundTripDeepB("deep"))))
    }

    @Test
    fun roundTripUnicodeAndMultilineStrings() {
        assertStableRoundTrip(RoundTripTexts("line1\nline2", "emoji \uD83D\uDE00 end"))
    }
}
