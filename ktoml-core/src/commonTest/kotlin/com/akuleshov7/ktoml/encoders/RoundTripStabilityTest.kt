package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Encode -> decode -> encode stability checks: the second canonical text must
 * be identical to the first one. All models use data classes whose property
 * order is fixed by declaration order (the encoder emits properties in
 * declaration order), so no sorting is applied anywhere in these tests.
 */
class RoundTripStabilityTest {
    @Serializable
    data class FlatPrimitives(val name: String, val version: Long, val active: Boolean, val ratio: Double)

    @Serializable
    data class Server(val host: String, val port: Long)

    @Serializable
    data class NestedConfig(val debug: Boolean, val server: Server)

    @Serializable
    data class DeepInner(val value: Long)

    @Serializable
    data class DeepMiddle(val inner: DeepInner, val label: String)

    @Serializable
    data class DeepOuter(val mid: DeepMiddle, val id: Long)

    @Serializable
    data class Item(val name: String, val qty: Long)

    @Serializable
    data class Basket(val items: List<Item>)

    @Serializable
    data class WithArrays(val tags: List<String>, val nums: List<Long>, val note: String)

    @Serializable
    data class Mixed(val title: String, val server: Server, val items: List<Item>)

    private fun <T> assertStableRoundTrip(model: T, serializer: KSerializer<T>, expectedText: String) {
        val firstText = Toml.encodeToString(serializer, model)
        assertEquals(expectedText, firstText, "first canonical encoding differs")
        val decoded = Toml.decodeFromString(serializer, firstText)
        assertEquals(model, decoded, "decode(encode(model)) must return the original model")
        val secondText = Toml.encodeToString(serializer, decoded)
        assertEquals(firstText, secondText, "second canonical encoding must be identical to the first")
    }

    @Test
    fun flatPrimitivesRoundTrip() {
        assertStableRoundTrip(
            FlatPrimitives("demo", 3, true, 2.5),
            FlatPrimitives.serializer(),
            """
                |name = "demo"
                |version = 3
                |active = true
                |ratio = 2.5
            """.trimMargin()
        )
    }

    @Test
    fun nestedTableRoundTrip() {
        assertStableRoundTrip(
            NestedConfig(true, Server("localhost", 8080)),
            NestedConfig.serializer(),
            """
                |debug = true
                |
                |[server]
                |    host = "localhost"
                |    port = 8080
            """.trimMargin()
        )
    }

    @Test
    fun deeplyNestedTableRoundTrip() {
        assertStableRoundTrip(
            DeepOuter(DeepMiddle(DeepInner(42), "core"), 7),
            DeepOuter.serializer(),
            """
                |id = 7
                |
                |[mid]
                |    label = "core"
                |
                |    [mid.inner]
                |        value = 42
            """.trimMargin()
        )
    }

    @Test
    fun arrayOfTablesRoundTrip() {
        assertStableRoundTrip(
            Basket(listOf(Item("apple", 1), Item("pear", 2))),
            Basket.serializer(),
            """
                |[[items]]
                |    name = "apple"
                |    qty = 1
                |
                |[[items]]
                |    name = "pear"
                |    qty = 2
            """.trimMargin()
        )
    }

    @Test
    fun primitiveArraysAndEscapedStringsRoundTrip() {
        assertStableRoundTrip(
            WithArrays(listOf("a", "b\"c"), listOf(1, 2, 3), "line\nbreak"),
            WithArrays.serializer(),
            """
                |tags = [ "a", "b\"c" ]
                |nums = [ 1, 2, 3 ]
                |note = "line\nbreak"
            """.trimMargin()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTablesRoundTrip() {
        assertStableRoundTrip(
            Mixed("t", Server("h", 1), listOf(Item("x", 9))),
            Mixed.serializer(),
            """
                |title = "t"
                |
                |[server]
                |    host = "h"
                |    port = 1
                |
                |[[items]]
                |    name = "x"
                |    qty = 9
            """.trimMargin()
        )
    }
}
