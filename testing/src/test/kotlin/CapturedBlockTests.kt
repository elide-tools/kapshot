import io.koalaql.kapshot.Capturable
import io.koalaql.kapshot.CapturedBlock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CapturedBlockTests {
    fun <T : Any> expectCapture(
        source: String,
        result: T,
        block: CapturedBlock<T>
    ) {
        assertEquals(result, block())
        assertEquals(source, block.source())
    }

    @Test
    fun `basic test cases`() {
        expectCapture(
            """5 + 7""",
            5 + 7
        ) { 5 + 7 }

        expectCapture(
"""val x = 10 + 5
val y = x * x

"" + (y + x)""",
            "240",
        ) {
            val x = 10 + 5
            val y = x * x

            "" + (y + x)
        }
    }

    @Suppress("MoveLambdaOutsideParentheses")
    @Test
    fun `explicit sam conversion support`() {
        expectCapture(
            """var i = 1

while (i < 100) {
    i *= 2
}

i""",
            128,
            CapturedBlock({
                var i = 1

                while (i < 100) {
                    i *= 2
                }

                i
            })
        )
    }

    /*
    Emojis here are used to test against possible source offset issues introduced by non-BMP Unicode
    🎅 🎅 🎅
    */

    @Test
    fun `capture nesting`() {
        expectCapture("""
            expectCapture("2 + 2", 4) {
                2 + 2
            }
        """.trimIndent(), Unit) {
            expectCapture("2 + 2", 4) {
                2 + 2
            }
        }
    }

    @Test
    fun `embedded string literal test`() {
        fun <T : Any> sourceOf(block: CapturedBlock<T>): String = block.source()

        assertEquals("""🎅|${sourceOf {
            2 + 2
        }}|""", "\uD83C\uDF85|2 + 2|")
    }

    fun interface CapturedTest1<T, R>: Capturable<CapturedTest1<T, R>> {
        operator fun invoke(arg: T): R

        override fun withSource(source: String): CapturedTest1<T, R> = object : CapturedTest1<T, R> by this {
            override fun source(): String = source
        }
    }

    @Test
    fun `user defined unary capture block`() {
        fun List<Int>.sourceyMap(block: CapturedTest1<Int, Int>): String {
            return "$this.map { ${block.source()} } = ${map { block(it) }}"
        }

        assertEquals(
            "[1, 2, 3].map { it*2 } = [2, 4, 6]",
            listOf(1, 2, 3).sourceyMap { it*2 }
        )
    }
}