# Testing Notes: Combined TOML Syntax Coverage

New commonTest coverage for combined dotted keys, inline tables, arrays of
tables, multiline strings and comments, plus encode-decode-encode stability.
Success cases assert both the AST tree structure and the decoded model;
failure cases assert the reported line number and key path.

## New tests

### `com.akuleshov7.ktoml.parsers.DottedKeyParserTestExtended`
- `quotedDottedSegmentDefinesOwnership` — `a."b.c".d = 1` tree + decode
- `literalQuotedDottedSegmentDefinesOwnership` — `a.'b.c'.d = 2` tree + decode
- `emptySegmentIsKeptByParserButRejectedOnWrite` — `a..b` parts kept, write rejected with key path
- `unclosedQuoteInDottedKeyFailsWithLineNumber` — asserts `Line 3` and the key
- `bareKeyWithSpacesFailsWithLineNumberAndKey` — asserts `Line 2` and `BAD KEY`
- `duplicateKeysAreKeptInTreeAndLastOneWinsOnDecode` — both nodes in tree, decode yields last value
- `commentsGluedToKeyValuePairs` — prepended/inline comments attached to the right node
- `multilineBasicStringUnderDottedKey` — tree ownership + decoded `\n` content
- `multilineLiteralStringKeepsNewlinesVerbatim` — literal multiline content
- `lineEndingBackslashConsumesNewlineAndLeadingWhitespace` — `\` + newline + indent collapses
- `unicodeSurrogatePairEscapeIsCombined` — `\uD83D\uDE00` decodes to a single emoji
- `endOfFileWithoutTrailingNewline` — `a = 1` at EOF, tree + decode

### `com.akuleshov7.ktoml.decoders.tables.NestedInlineTableTestExtended`
- `inlineTableWithNestedArrayAndNestedTable` — `{ b = [1, 2], c = { d = [3] } }` tree + decode
- `dottedKeyHoldingInlineTableWithArray` — `a.b = { c = 1, d = [2, 3] }` tree + partial decode of `a.b`
- `inlineTableInsideArrayOfTables` — inline table attributed to the latest `[[fruits]]` element
- `inlineTableAtEndOfFileWithoutNewline` — `a = { b = 1 }` at EOF, tree + decode

### `com.akuleshov7.ktoml.parsers.ArraysOfTablesTestExtended`
- `subTableAfterElementBelongsToLatestElement` — `[fruits.physical]` stays in the first `[[fruits]]`
- `dottedKeyInsideArrayOfTablesStaysInLatestElement` — `physical.color` per element
- `arrayOfTablesAtEndOfFileWithoutNewline` — `[[f]]` element at EOF
- `multilineBasicStringInsideArrayOfTables` — per-element multiline values
- `duplicateKeyInsideSingleArrayElement` — both nodes kept, decode yields last value

### `com.akuleshov7.ktoml.encoders.RoundTripStabilityTest`
Encode -> decode -> encode; the second canonical text must equal the first.
Models use declaration-ordered properties only (the encoder's committed
order); no sorting is applied in the tests.
- `flatPrimitivesRoundTrip`
- `nestedTableRoundTrip`
- `deeplyNestedTableRoundTrip`
- `arrayOfTablesRoundTrip`
- `primitiveArraysAndEscapedStringsRoundTrip`
- `mixedTablesAndArrayOfTablesRoundTrip`

## Verification

- Acceptance (from repo root, exit code 0):
  `./gradlew :ktoml-core:jvmTest --tests '*DottedKeyParserTest*' --tests '*NestedInlineTableTest*' --tests '*ArraysOfTablesTest*'`
- JS target compiles and passes: `./gradlew :ktoml-core:compileTestKotlinJs` and `jsNodeTest`.
- Pre-existing environment issue (unrelated): `com.akuleshov7.ktoml.compliance.TomlTestSuite`
  fails in a full `jvmTest` run because the optional `toml-test` git submodule
  is not initialized; the new tests do not depend on it.

## Mutation checks performed (source reverted afterwards)

1. Dotted segment attribution: forced `.` to split inside quoted keys in
   `splitKeyToTokens` (`parsers/StringUtils.kt`). New failures:
   `quotedDottedSegmentDefinesOwnership`, `literalQuotedDottedSegmentDefinesOwnership`.
2. Multiline escape newline consumption: kept the newline in
   `convertLineEndingBackslash` (`parsers/StringUtils.kt`). New failure:
   `lineEndingBackslashConsumesNewlineAndLeadingWhitespace`.
