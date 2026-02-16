package com.amrts.regexsmali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmaliRegexConverterTest {

    private lateinit var converter: SmaliRegexConverter

    @Before
    fun setUp() {
        converter = SmaliRegexConverter()
    }

    private fun basicConvert(input: String) =
        converter.convert(input, ConversionOptions())

    private fun advancedConvert(input: String) =
        converter.convert(input, ConversionOptions(advancedWhitespaceMode = true))

    private fun branchConvert(input: String) =
        converter.convert(input, ConversionOptions(includeBranches = true))

    private fun debugExcludeConvert(input: String) =
        converter.convert(input, ConversionOptions(excludeDebugInfo = true))

    // --- Basic Escaping ---

    @Test
    fun `escapes dash characters`() {
        val result = basicConvert("move-result v0")
        assertTrue(result.contains("\\-"))
    }

    @Test
    fun `escapes curly braces`() {
        val result = basicConvert("invoke-static {}, Lcom/example/Foo;->bar()V")
        assertTrue(result.contains("\\{"))
        assertTrue(result.contains("\\}"))
    }

    @Test
    fun `escapes parentheses`() {
        val result = basicConvert("Lcom/example/Foo;->bar()V")
        assertTrue(result.contains("\\("))
        assertTrue(result.contains("\\)"))
    }

    @Test
    fun `escapes dot characters`() {
        val result = basicConvert(".method public static main()V")
        assertTrue(result.contains("\\."))
    }

    @Test
    fun `escapes backslash`() {
        val result = basicConvert("\\test")
        assertTrue(result.contains("\\\\"))
    }

    // --- Register Replacement ---

    @Test
    fun `replaces registers in move-result`() {
        val result = basicConvert("    move-result v0")
        assertTrue(result.contains("([pv]\\d+)"))
        assertTrue(!result.contains("v0"))
    }

    @Test
    fun `replaces p-registers`() {
        val result = basicConvert("    move-result p1")
        assertTrue(result.contains("([pv]\\d+)"))
        assertTrue(!result.contains("p1"))
    }

    @Test
    fun `replaces registers in invoke with braces`() {
        val result = basicConvert("invoke-virtual {v0, v1}, Lcom/Foo;->bar(II)V")
        val count = Regex("\\(\\[pv\\]\\\\d\\+\\)").findAll(result).count()
        assertEquals(2, count)
    }

    @Test
    fun `replaces register after opcode`() {
        val result = basicConvert("    const/4 v0, 0x0")
        assertTrue(result.contains("([pv]\\d+)"))
    }

    @Test
    fun `does not replace register-like patterns in class names`() {
        val result = basicConvert("Lcom/example/v0handler;")
        assertTrue(!result.contains("([pv]\\d+)"))
    }

    // --- Directive Number Pattern ---

    @Test
    fun `converts dot-registers directive`() {
        val result = basicConvert(".registers 12")
        assertEquals("\\.registers \\d+", result)
    }

    @Test
    fun `converts dot-locals directive`() {
        val result = basicConvert(".locals 3")
        assertEquals("\\.locals \\d+", result)
    }

    // --- Advanced Whitespace Mode ---

    @Test
    fun `advanced mode replaces leading spaces`() {
        val result = advancedConvert("    invoke-static {}, Lcom/Foo;->bar()V")
        assertTrue(result.startsWith("\\s{4}"))
    }

    @Test
    fun `advanced mode joins lines with newline markers`() {
        val result = advancedConvert("    line1\n    line2")
        assertTrue(result.contains("\\n"))
    }

    @Test
    fun `advanced mode handles blank lines with newline count`() {
        val result = advancedConvert("line1\n\nline2")
        assertTrue(result.contains("\\n{2}"))
    }

    @Test
    fun `advanced mode handles multiple blank lines`() {
        val result = advancedConvert("line1\n\n\nline2")
        assertTrue(result.contains("\\n{3}"))
    }

    // --- Branch Labels ---

    @Test
    fun `branch labels not converted by default`() {
        val result = basicConvert(":cond_a1")
        assertEquals(":cond_a1", result)
    }

    @Test
    fun `standalone branch labels converted when enabled`() {
        val result = branchConvert(":cond_a1")
        assertEquals(":[a-zA-Z_]\\w*", result)
    }

    @Test
    fun `inline branch label refs converted when enabled`() {
        val result = branchConvert("    if-ltz v0, :cond_a1")
        assertTrue(result.contains(":[a-zA-Z_]\\w*"))
        assertTrue(!result.contains(":cond_a1"))
    }

    @Test
    fun `goto labels converted when enabled`() {
        val result = branchConvert(":goto_b2")
        assertEquals(":[a-zA-Z_]\\w*", result)
    }

    // --- Debug Info Exclusion ---

    @Test
    fun `excludes dot-line directives`() {
        val result = debugExcludeConvert("    .line 42\n    move v0, v1")
        assertTrue(!result.contains("line"))
        assertTrue(result.contains("move"))
    }

    @Test
    fun `excludes dot-local directives`() {
        val result = debugExcludeConvert("    .local v0, \"x\":I\n    return v0")
        assertTrue(!result.contains("local"))
        assertTrue(result.contains("return"))
    }

    @Test
    fun `excludes prologue`() {
        val result = debugExcludeConvert("    .prologue\n    const/4 v0, 0x0")
        assertTrue(!result.contains("prologue"))
        assertTrue(result.contains("const"))
    }

    @Test
    fun `excludes source directive`() {
        val result = debugExcludeConvert("    .source \"Foo.java\"\n    return-void")
        assertTrue(!result.contains("source"))
    }

    @Test
    fun `preserves non-debug lines when excluding`() {
        val input = "    .line 1\n    invoke-static {}, Lcom/Foo;->bar()V\n    .line 2\n    return-void"
        val result = debugExcludeConvert(input)
        assertTrue(result.contains("invoke"))
        assertTrue(result.contains("return"))
        assertTrue(!result.contains("line"))
    }

    // --- Combined Options ---

    @Test
    fun `advanced mode with branches`() {
        val options = ConversionOptions(advancedWhitespaceMode = true, includeBranches = true)
        val result = converter.convert("    if-ltz v0, :cond_0\n    return v0\n\n:cond_0", options)
        assertTrue(result.contains("\\s{4}"))
        assertTrue(result.contains(":[a-zA-Z_]\\w*"))
        assertTrue(result.contains("\\n"))
    }

    @Test
    fun `all options enabled`() {
        val options = ConversionOptions(
            advancedWhitespaceMode = true,
            includeBranches = true,
            excludeDebugInfo = true
        )
        val input = "    .line 1\n    if-ltz v0, :cond_0\n    .prologue\n    return v0"
        val result = converter.convert(input, options)
        assertTrue(!result.contains("line"))
        assertTrue(!result.contains("prologue"))
        assertTrue(result.contains(":[a-zA-Z_]\\w*"))
        assertTrue(result.contains("\\s{4}"))
    }

    // --- Edge Cases ---

    @Test
    fun `empty input returns empty`() {
        assertEquals("", basicConvert(""))
    }

    @Test
    fun `blank input returns empty content`() {
        val result = basicConvert("   ")
        assertTrue(result.trim().isEmpty())
    }

    @Test
    fun `single register on opcode line`() {
        val result = basicConvert("    return v0")
        assertTrue(result.contains("([pv]\\d+)"))
    }

    @Test
    fun `multiline input preserves line count`() {
        val input = "line1\nline2\nline3"
        val result = basicConvert(input)
        assertEquals(3, result.split("\n").size)
    }

    @Test
    fun `preserves leading whitespace in basic mode`() {
        val result = basicConvert("    some-opcode v0")
        assertTrue(result.startsWith("    "))
    }
}
