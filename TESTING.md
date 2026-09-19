# Testing: combined TOML syntax coverage (dotted keys, inline tables, arrays of tables, multiline strings, comments)

All tests live in `ktoml-core/src/commonTest` and run on JVM and JS (node) without
external fixtures or services.

## Acceptance commands

```bash
# preparation (not part of the demo)
./gradlew :ktoml-core:compileKotlinJvm

# acceptance, run from the repository root
./gradlew :ktoml-core:jvmTest --tests '*DottedKeyParserTest*' --tests '*NestedInlineTableTest*' --tests '*ArraysOfTablesTest*'
```

Optional JS check:

```bash
./gradlew :ktoml-core:jsNodeTest --tests '*DottedKeyParserTest*' --tests '*NestedInlineTableTest*' --tests '*ArraysOfTablesTest*'
```

## New tests

### `com.akuleshov7.ktoml.parsers.DottedKeyParserTest` (8 new)

- `quotedDottedSegmentWithDotsInsideQuotes` — quoted dotted segment `a."b.c".d`, tree + decode
- `quotedDottedSegmentUnderQuotedTableHeader` — quoted header `["a.b.c"]` + dotted key below it, tree + decode
- `emptyMiddleSegmentIsRejected` — `a..b = 1` fails, message contains the key path `[a, ]`
- `trailingEmptySegmentIsRejected` — `a. = 1` fails, message mentions the empty key part
- `duplicateKeyDecodeKeepsLastValue` — pins current duplicate-key semantics (both nodes in tree, last value decoded)
- `bareKeyWithSpacesFailsWithLineNumberAndKey` — `ParseException`, asserts line number (`Line 3:`) and key (`[a b]`)
- `unclosedQuoteInKeyFailsWithLineNumber` — `ParseException`, asserts line number (`Line 2:`) and key
- `endOfFileWithoutTrailingNewline` — input without trailing newline, tree + decode

### `com.akuleshov7.ktoml.parsers.ArraysOfTablesTest` (4 new)

- `subtableAfterFirstArrayElement` — `[fruits.physical]` after the first `[[fruits]]` element, tree + decode
- `subtableAttachesToLatestArrayElement` — sub-table binds to the most recent array element, tree + decode
- `dottedKeyInsideArrayOfTablesElement` — `physical.color = "red"` inside an element, tree + decode
- `inlineTableInsideArrayOfTablesElement` — `physical = { ... }` inside an element, tree + decode

### `com.akuleshov7.ktoml.parsers.NestedInlineTableTest` (16 new)

- `inlineTableWithNestedArray` — `t = { b = [1, 2], c = { d = "x" } }`, tree + decode
- `inlineTableWithDottedKey` — dotted key inside an inline table, tree + decode
- `multilineBasicStringWithLineEndingBackslash` — line-ending `\` consumes the newline and leading whitespace
- `multilineLiteralStringPreservesContent` — literal multiline string keeps content/backslashes
- `multilineBasicStringWithEscapedChars` — `\t`, `é`, `\U0001F600` (surrogate pair) in multiline basic string
- `basicStringWithUnicodeSurrogatePairEscape` — `\U0001F600` decodes to the surrogate pair
- `unicodeEscapeInQuotedKeyAndValue` — `"\u00E9"` as quoted key and in value
- `emojiSurrogatePairInQuotedKey` — emoji (surrogate pair) as a quoted key
- `commentsGluedToValuesAndKeys` — comments glued to values/keys (`b = 2# comment`), tree + decode
- `multilineStringAtEofWithoutTrailingNewline` — multiline string ending exactly at EOF
- `roundTripPrimitives` — encode → decode → encode, canonical text of both encodings is identical
- `roundTripNestedTables` — same, nested tables
- `roundTripArrayOfTables` — same, `[[fruits]]`
- `roundTripPrimitiveArrays` — same, arrays of primitives
- `roundTripDeeplyNestedTables` — same, `[a.b]` style nesting
- `roundTripUnicodeAndMultilineStrings` — same, strings with `\n` and emoji

Round-trip tests rely on the library's existing promise that properties are
encoded in declaration order; the assertions compare the full canonical text
without any re-sorting.

## Mutation checks (performed, source restored afterwards)

1. **Dotted segment attribution** — in
   `ktoml-core/src/commonMain/kotlin/com/akuleshov7/ktoml/tree/nodes/pairs/TomlKeyValue.kt`
   (`createTomlTableFromDottedKey`), `TomlKey(parentalPrefix + syntheticTablePrefix)`
   was replaced with `TomlKey(syntheticTablePrefix)`.
   Result: 6 failures, including the new tests
   `quotedDottedSegmentUnderQuotedTableHeader`, `dottedKeyInsideArrayOfTablesElement`,
   `inlineTableWithDottedKey`.
2. **Multiline escape newline consumption** — in
   `ktoml-core/src/commonMain/kotlin/com/akuleshov7/ktoml/parsers/StringUtils.kt`
   (`convertLineEndingBackslash`), `joinToString("") { it.trimStart() }` was
   replaced with `joinToString("") { it }`.
   Result: 1 failure, the new test `multilineBasicStringWithLineEndingBackslash`.

Both mutations were reverted with `git checkout`; the working tree contains only
the new/changed test files and this document.
