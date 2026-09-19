package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.Toml.Default.tomlParser
import kotlinx.serialization.decodeFromString
import com.akuleshov7.ktoml.tree.nodes.TomlArrayOfTablesElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArraysOfTablesTest {
    @Test
    fun positiveSimpleParsing() {
        val string = """
            [[fruits]]
            a = "apple"
            b = "qwerty"

            [[fruits]]
            a = "banana"
            b = "qwerty"

            [[fruits]]
            a = "plantain"
            b = "qwerty"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[fruits]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (a="apple")
                |             - TomlKeyValuePrimitive (b="qwerty")
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (a="banana")
                |             - TomlKeyValuePrimitive (b="qwerty")
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (a="plantain")
                |             - TomlKeyValuePrimitive (b="qwerty")
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parsingOfEmptyArrayOfTables() {
        val string = """
            [[products]]
                name = "Hammer"
                sku = 738594937
        
            [[products]]  
        
            [[products]]
                name = "Nail"
                sku = 284758393
            
                color = "gray"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[products]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name="Hammer")
                |             - TomlKeyValuePrimitive (sku=738594937)
                |         - TomlArrayOfTablesElement (technical_node)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name="Nail")
                |             - TomlKeyValuePrimitive (sku=284758393)
                |             - TomlKeyValuePrimitive (color="gray")
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parsingNestedArraysOfTables1() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties.inside]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([fruits])
                |         - TomlTable ([[fruits.varieties]])
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name="red delicious")
                |                 - TomlTable ([[fruits.varieties.inside]])
                |                     - TomlArrayOfTablesElement (technical_node)
                |                         - TomlKeyValuePrimitive (name="granny smith")
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parsingNestedArraysOfTables2() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [fruits.varieties.inside]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([fruits])
                |         - TomlTable ([[fruits.varieties]])
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name="red delicious")
                |                 - TomlTable ([fruits.varieties.inside])
                |                     - TomlKeyValuePrimitive (name="granny smith")
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parsingSimpleArrayOfTables1() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits.varieties]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        val fruitVarieties = parsedToml.children.last().children.last()
        assertEquals(3, fruitVarieties.children.size)
        fruitVarieties.children.forEach {
            assertTrue { it is TomlArrayOfTablesElement }
        }
    }

    @Test
    fun parsingSimpleArrayOfTables2() {
        //    FixMe: here should throw an exception in case of table duplication https://github.com/akuleshov7/ktoml/issues/30
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [fruits.varieties]
                name = "granny smith"
                
            [a]
                b = 1
            
            [[a]]
                b = 1
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
    }

    @Test
    fun parsingSimpleArrayOfTables3() {
        val string = """
            [[a]] 
            [[b]]
            [[a.b]]
            [[a.b]]
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """ 
                | - TomlFile (rootNode)
                |     - TomlTable ([[a]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([[a.b]])
                |                 - TomlArrayOfTablesElement (technical_node)
                |                 - TomlArrayOfTablesElement (technical_node)
                |     - TomlTable ([[b]])
                |         - TomlArrayOfTablesElement (technical_node)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )

    }

    @Test
    fun parsingSimpleArrayOfTables4() {
        val string = """
            [[a]] 
            [[b]]
            [[a.b]]
            [[a]]
            [[a.b]]
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """ 
                | - TomlFile (rootNode)
                |     - TomlTable ([[a]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([[a.b]])
                |                 - TomlArrayOfTablesElement (technical_node)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([[a.b]])
                |                 - TomlArrayOfTablesElement (technical_node)
                |     - TomlTable ([[b]])
                |         - TomlArrayOfTablesElement (technical_node)
                |
                """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun tomlExampleParsingTest() {
        val string = """
            [[fruits]]
                name = "apple"
            
            # this is processed incorrectly now
            [fruits.physical]
                color = "red"
                shape = "round"
            
            # this is processed incorrectly now
            [fruits.physical.inside]
                color = "red"
                shape = "round"
               
            # this is processed incorrectly now
            [vegetables]
                outSideOfArray = true
            
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits]]
                name = "banana"
            
            [[fruits.varieties]]
                name = "plantain"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """ 
                    | - TomlFile (rootNode)
                    |     - TomlTable ([[fruits]])
                    |         - TomlArrayOfTablesElement (technical_node)
                    |             - TomlKeyValuePrimitive (name="apple")
                    |             - TomlTable ([fruits.physical])
                    |                 - TomlKeyValuePrimitive (color="red")
                    |                 - TomlKeyValuePrimitive (shape="round")
                    |                 - TomlTable ([fruits.physical.inside])
                    |                     - TomlKeyValuePrimitive (color="red")
                    |                     - TomlKeyValuePrimitive (shape="round")
                    |             - TomlTable ([[fruits.varieties]])
                    |                 - TomlArrayOfTablesElement (technical_node)
                    |                     - TomlKeyValuePrimitive (name="red delicious")
                    |                 - TomlArrayOfTablesElement (technical_node)
                    |                     - TomlKeyValuePrimitive (name="granny smith")
                    |         - TomlArrayOfTablesElement (technical_node)
                    |             - TomlKeyValuePrimitive (name="banana")
                    |             - TomlTable ([[fruits.varieties]])
                    |                 - TomlArrayOfTablesElement (technical_node)
                    |                     - TomlKeyValuePrimitive (name="plantain")
                    |     - TomlTable ([vegetables])
                    |         - TomlKeyValuePrimitive (outSideOfArray=true)
                    |
            """.trimMargin(), parsedToml.prettyStr()
        )
    }

    @Test
    fun nestedArraysOnly1() {
        val string = """
            [[a]]
                name = 1
            
            [[a.b]]
                name = 2
            
            [[a]]
                name = 3
            
            [[a.b]]
                name = 4
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
            | - TomlFile (rootNode)
            |     - TomlTable ([[a]])
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=1)
            |             - TomlTable ([[a.b]])
            |                 - TomlArrayOfTablesElement (technical_node)
            |                     - TomlKeyValuePrimitive (name=2)
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=3)
            |             - TomlTable ([[a.b]])
            |                 - TomlArrayOfTablesElement (technical_node)
            |                     - TomlKeyValuePrimitive (name=4)
            |     - TomlTable ([[c]])
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=5)
            |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun nestedArraysOnly2() {
        val string = """
            [[a]]
                name = 1
            
            [[a.b.c]]
                name = 2
            
            [[a]]
                name = 3
            
            [[a.b.c]]
                name = 4
                
            [[a.b.c.d]]
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()


    }

    @Test
    fun nestedArraysOnly3() {
        val string = """
           [[a.b]]
               [[a.b.c]]
               [[a.b.c]]
                  a = 1
                  [[a.b.c.d.e.f]]
           [[a.b]]
               # TABLE HERE, NOT ARRAY:
               [a.b.c]
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                        | - TomlFile (rootNode)
                        |     - TomlTable ([a])
                        |         - TomlTable ([[a.b]])
                        |             - TomlArrayOfTablesElement (technical_node)
                        |                 - TomlTable ([[a.b.c]])
                        |                     - TomlArrayOfTablesElement (technical_node)
                        |                     - TomlArrayOfTablesElement (technical_node)
                        |                         - TomlKeyValuePrimitive (a=1)
                        |                         - TomlTable ([a.b.c.d])
                        |                             - TomlTable ([a.b.c.d.e])
                        |                                 - TomlTable ([[a.b.c.d.e.f]])
                        |                                     - TomlArrayOfTablesElement (technical_node)
                        |             - TomlArrayOfTablesElement (technical_node)
                        |                 - TomlTable ([a.b.c])
                        |                     - TomlStubEmptyNode (technical_node)
                        |
        """.trimMargin(), parsedToml.prettyStr()
        )
    }

    @Test
    fun parsingNestedArraysOfTablesRegression() {
        val string = """
            [[a]]
                name = 1
            
            [a.b]
                name = 2
            
            [[a]]
                name = 3
            
            [a.b]
                name = 4
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[a]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=1)
                |             - TomlTable ([a.b])
                |                 - TomlKeyValuePrimitive (name=2)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=3)
                |             - TomlTable ([a.b])
                |                 - TomlKeyValuePrimitive (name=4)
                |     - TomlTable ([[c]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=5)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun testForStubTables() {
        val string = """
            [[a]]
              [a.b]
            [[a]]
              [a.b]
            [[a]]
              [a.b]
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[a]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([a.b])
                |                 - TomlStubEmptyNode (technical_node)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([a.b])
                |                 - TomlStubEmptyNode (technical_node)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([a.b])
                |                 - TomlStubEmptyNode (technical_node)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTables1() {
        val string = """
            [[a.b]]
                name = 1
            
            [a.b.c]
                name = 2
            
            [[a.b]]
                name = 3
            
            [a.b.c]
                name = 4
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([a])
                |         - TomlTable ([[a.b]])
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name=1)
                |                 - TomlTable ([a.b.c])
                |                     - TomlKeyValuePrimitive (name=2)
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name=3)
                |                 - TomlTable ([a.b.c])
                |                     - TomlKeyValuePrimitive (name=4)
                |     - TomlTable ([[c]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=5)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTables2() {
        val string = """
            [a]
                name = 1
            
            [[a.b.c]]
                name = 2
            
            [[a.b.c]]
                name = 4
        """.trimIndent()


        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([a])
                |         - TomlKeyValuePrimitive (name=1)
                |         - TomlTable ([a.b])
                |             - TomlTable ([[a.b.c]])
                |                 - TomlArrayOfTablesElement (technical_node)
                |                     - TomlKeyValuePrimitive (name=2)
                |                 - TomlArrayOfTablesElement (technical_node)
                |                     - TomlKeyValuePrimitive (name=4)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTables3() {
        val string = """
        [[fruit]]
        [fruit.physical]  
        color = "red"
        shape = "round"
        
        [[fruit]]
        [fruit.physical]  
        color = "red"
        shape = "round"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[fruit]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([fruit.physical])
                |                 - TomlKeyValuePrimitive (color="red")
                |                 - TomlKeyValuePrimitive (shape="round")
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([fruit.physical])
                |                 - TomlKeyValuePrimitive (color="red")
                |                 - TomlKeyValuePrimitive (shape="round")
                |
               """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun dottedKeysInsideTable() {
        val string = """
            [[table]]
            simple = 1
            item.simple = 2
            
            [[table]]
            item.complex = { a = 3, b = 4 }

            [[table]]
            simple = 5
            item.simple = 6
            item.complex = { a = 7, b = 8 }
        """.trimIndent()
        val parsedToml = tomlParser.parseString(string)

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[table]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (simple=1)
                |             - TomlTable ([table.item])
                |                 - TomlKeyValuePrimitive (simple=2)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([table.item])
                |                 - TomlTable ([table.item.complex])
                |                     - TomlKeyValuePrimitive (a=3)
                |                     - TomlKeyValuePrimitive (b=4)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (simple=5)
                |             - TomlTable ([table.item])
                |                 - TomlKeyValuePrimitive (simple=6)
                |                 - TomlTable ([table.item.complex])
                |                     - TomlKeyValuePrimitive (a=7)
                |                     - TomlKeyValuePrimitive (b=8)
                |
               """.trimMargin(),
            parsedToml.prettyStr()
        )
    }
}

/**
 * Combined coverage for arrays of tables interacting with following
 * sub-tables, dotted keys, multiline strings, duplicate keys and
 * end-of-file boundaries.
 */
class ArraysOfTablesTestExtended {
    @kotlinx.serialization.Serializable
    data class Physical(val color: String)

    @kotlinx.serialization.Serializable
    data class Fruit(val name: String, val physical: Physical? = null)

    @kotlinx.serialization.Serializable
    data class Basket(val fruits: List<Fruit>)

    @kotlinx.serialization.Serializable
    data class ColoredFruit(val physical: Physical)

    @kotlinx.serialization.Serializable
    data class ColoredBasket(val fruits: List<ColoredFruit>)

    @kotlinx.serialization.Serializable
    data class SimpleElement(val a: Long)

    @kotlinx.serialization.Serializable
    data class SimpleBucket(val f: List<SimpleElement>)

    @kotlinx.serialization.Serializable
    data class TextElement(val s: String)

    @kotlinx.serialization.Serializable
    data class TextBucket(val f: List<TextElement>)

    @Test
    fun subTableAfterElementBelongsToLatestElement() {
        val input = """
            [[fruits]]
            name = "apple"
            [fruits.physical]
            color = "red"
            [[fruits]]
            name = "banana"
        """.trimIndent()
        val parsedToml = tomlParser.parseString(input)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[fruits]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name="apple")
                |             - TomlTable ([fruits.physical])
                |                 - TomlKeyValuePrimitive (color="red")
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name="banana")
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            Basket(listOf(Fruit("apple", Physical("red")), Fruit("banana"))),
            Toml.decodeFromString<Basket>(input)
        )
    }

    @Test
    fun dottedKeyInsideArrayOfTablesStaysInLatestElement() {
        val input = """
            [[fruits]]
            physical.color = "red"
            [[fruits]]
            physical.color = "green"
        """.trimIndent()
        val parsedToml = tomlParser.parseString(input)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[fruits]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([fruits.physical])
                |                 - TomlKeyValuePrimitive (color="red")
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlTable ([fruits.physical])
                |                 - TomlKeyValuePrimitive (color="green")
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            ColoredBasket(listOf(ColoredFruit(Physical("red")), ColoredFruit(Physical("green")))),
            Toml.decodeFromString<ColoredBasket>(input)
        )
    }

    @Test
    fun arrayOfTablesAtEndOfFileWithoutNewline() {
        val input = "[[f]]\na = 1"
        val parsedToml = tomlParser.parseString(input)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[f]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (a=1)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            SimpleBucket(listOf(SimpleElement(1))),
            Toml.decodeFromString<SimpleBucket>(input)
        )
    }

    @Test
    fun multilineBasicStringInsideArrayOfTables() {
        val input = "[[f]]\ns = \"\"\"l1\nl2\"\"\"\n[[f]]\ns = \"\"\"m1\"\"\""
        val parsedToml = tomlParser.parseString(input)
        val table = parsedToml.children.single() as com.akuleshov7.ktoml.tree.nodes.TomlTable
        assertEquals(2, table.children.size)
        val first = table.children[0].children.single() as com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
        val second = table.children[1].children.single() as com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
        assertEquals("l1\nl2", first.value.content)
        assertEquals("m1", second.value.content)
        assertEquals(
            TextBucket(listOf(TextElement("l1\nl2"), TextElement("m1"))),
            Toml.decodeFromString<TextBucket>(input)
        )
    }

    @Test
    fun duplicateKeyInsideSingleArrayElement() {
        val input = "[[f]]\na = 1\na = 2"
        val parsedToml = tomlParser.parseString(input)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable ([[f]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (a=1)
                |             - TomlKeyValuePrimitive (a=2)
                |
            """.trimMargin(),
            parsedToml.prettyStr()
        )
        assertEquals(
            SimpleBucket(listOf(SimpleElement(2))),
            Toml.decodeFromString<SimpleBucket>(input)
        )
    }
}
