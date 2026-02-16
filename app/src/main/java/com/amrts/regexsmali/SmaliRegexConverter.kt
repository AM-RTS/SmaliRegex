package com.amrts.regexsmali

data class ConversionOptions(
    val advancedWhitespaceMode: Boolean = false,
    val includeBranches: Boolean = false,
    val excludeDebugInfo: Boolean = false
)

class SmaliRegexConverter {

    fun convert(input: String, options: ConversionOptions): String {
        var lines = input.split("\n")
        if (options.excludeDebugInfo) {
            lines = lines.filter { line -> !isDebugLine(line.trim()) }
        }
        val converted = lines.map { line -> convertLine(line, options.includeBranches) }

        return if (options.advancedWhitespaceMode) {
            buildAdvancedOutput(converted)
        } else {
            buildBasicOutput(converted)
        }
    }

    private fun convertLine(line: String, includeBranches: Boolean): ConvertedLine {
        val leadingSpaces = line.length - line.trimStart(' ').length
        val trimmed = line.trimStart(' ')
        val escaped = escapeAndReplace(trimmed, includeBranches)
        return ConvertedLine(leadingSpaces, escaped)
    }

    private fun escapeAndReplace(line: String, includeBranches: Boolean): String {
        if (line.isBlank()) return ""

        if (BRANCH_LABEL_LINE_PATTERN.matches(line)) {
            return if (includeBranches) ":[a-zA-Z_]\\w*" else escapeRegexChars(line)
        }

        val directiveMatch = DIRECTIVE_NUMBER_PATTERN.find(line)
        if (directiveMatch != null) {
            val directive = escapeRegexChars(directiveMatch.groupValues[1])
            return "$directive \\d+"
        }

        return tokenize(line, includeBranches).joinToString("")
    }

    private fun tokenize(line: String, includeBranches: Boolean): List<String> {
        val replacements = mutableListOf<Replacement>()

        for (match in REGISTER_PATTERN.findAll(line)) {
            val registerGroup = match.groups[2]!!
            if (isRegisterContext(line, registerGroup.range)) {
                replacements.add(Replacement(registerGroup.range, "([pv]\\d+)"))
            }
        }

        if (includeBranches) {
            val existingRanges = replacements.map { it.range }
            for (match in BRANCH_LABEL_REF_PATTERN.findAll(line)) {
                if (!isInsideExistingReplacement(match.range, existingRanges)) {
                    replacements.add(Replacement(match.range, ":[a-zA-Z_]\\w*"))
                }
            }
        }

        replacements.sortBy { it.range.first }

        val parts = mutableListOf<String>()
        var lastEnd = 0

        for (rep in replacements) {
            if (rep.range.first > lastEnd) {
                parts.add(escapeRegexChars(line.substring(lastEnd, rep.range.first)))
            }
            parts.add(rep.replacement)
            lastEnd = rep.range.last + 1
        }

        if (lastEnd < line.length) {
            parts.add(escapeRegexChars(line.substring(lastEnd)))
        }

        return parts.ifEmpty { listOf(escapeRegexChars(line)) }
    }

    private fun isRegisterContext(line: String, range: IntRange): Boolean {
        val start = range.first
        val end = range.last

        val braceOpen = line.lastIndexOf('{', start)
        val braceClose = line.indexOf('}', end)
        if (braceOpen != -1 && braceClose != -1 && braceOpen < start && braceClose > end) {
            return true
        }

        val before = line.substring(0, start)

        if (before.matches(TRAILING_COMMA_SPACE_PATTERN)) return true

        if (before.matches(TRAILING_SPACE_PATTERN) && ',' !in before) {
            val trimmedBefore = before.trimEnd()
            if (trimmedBefore.isNotEmpty()) {
                val lastWord = trimmedBefore.split(WHITESPACE_PATTERN).last()
                if (isSmaliOpcode(lastWord) || lastWord.endsWith(",")) return true
            }
        }

        if (start == 0) return false

        return line[start - 1] in REGISTER_CONTEXT_CHARS
    }

    private fun isSmaliOpcode(word: String): Boolean {
        val lower = word.lowercase()
        return OPCODE_PREFIXES.any { lower == it || lower.startsWith("$it-") || lower.startsWith("$it/") }
    }

    private fun isDebugLine(trimmedLine: String): Boolean {
        return DEBUG_DIRECTIVES.any { directive ->
            trimmedLine == directive || trimmedLine.startsWith("$directive ")
        }
    }

    private fun isInsideExistingReplacement(range: IntRange, existingRanges: List<IntRange>): Boolean {
        return existingRanges.any { it.first <= range.first && it.last >= range.last }
    }

    private fun escapeRegexChars(input: String): String {
        val builder = StringBuilder(input.length)
        for (char in input) {
            val escaped = REGEX_ESCAPE_MAP[char]
            if (escaped != null) builder.append(escaped) else builder.append(char)
        }
        return builder.toString()
    }

    private fun buildBasicOutput(lines: List<ConvertedLine>): String {
        return lines.joinToString("\n") { " ".repeat(it.leadingSpaces) + it.content }
    }

    private fun buildAdvancedOutput(lines: List<ConvertedLine>): String {
        val parts = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.content.isEmpty()) {
                val blankCount = countConsecutiveBlanks(lines, i)
                val newlineCount = blankCount + 1
                parts.add(if (newlineCount >= 2) "\\n{$newlineCount}" else "\\n")
                i += blankCount
                continue
            }

            if (parts.isNotEmpty() && !parts.last().startsWith("\\n")) {
                parts.add("\\n")
            }

            val prefix = if (line.leadingSpaces > 0) "\\s{${line.leadingSpaces}}" else ""
            parts.add("$prefix${line.content}")
            i++
        }
        return parts.joinToString("")
    }

    private fun countConsecutiveBlanks(lines: List<ConvertedLine>, startIndex: Int): Int {
        var count = 0
        var i = startIndex
        while (i < lines.size && lines[i].content.isEmpty()) {
            count++
            i++
        }
        return count
    }

    private data class ConvertedLine(val leadingSpaces: Int, val content: String)

    private data class Replacement(val range: IntRange, val replacement: String)

    companion object {
        private val REGISTER_PATTERN = Regex("""(^|[^a-zA-Z])([pv]\d+)""")
        private val DIRECTIVE_NUMBER_PATTERN = Regex("""^(\.\w+)\s+(\d+)$""")
        private val BRANCH_LABEL_REF_PATTERN = Regex("""(:[a-zA-Z_]\w*)""")
        private val BRANCH_LABEL_LINE_PATTERN = Regex("""^(:[a-zA-Z_]\w*)$""")
        private val TRAILING_COMMA_SPACE_PATTERN = Regex(""".*,\s*$""")
        private val TRAILING_SPACE_PATTERN = Regex(""".*\s+$""")
        private val WHITESPACE_PATTERN = Regex("""\s+""")

        private val REGISTER_CONTEXT_CHARS = setOf(' ', ',', '{')

        private val OPCODE_PREFIXES = setOf(
            "invoke", "move", "return", "const", "iget", "iput", "sget", "sput",
            "aget", "aput", "new", "check", "instance", "if", "goto", "throw",
            "monitor", "fill", "packed", "sparse", "cmp", "neg", "not", "int",
            "long", "float", "double", "add", "sub", "mul", "div", "rem",
            "and", "or", "xor", "shl", "shr", "ushr", "rsub", "array"
        )

        private val DEBUG_DIRECTIVES = setOf(
            ".line", ".local", ".end local", ".restart local",
            ".prologue", ".epilogue", ".source", ".param", ".end param"
        )

        private val REGEX_ESCAPE_MAP = mapOf(
            '-' to "\\-", '{' to "\\{", '}' to "\\}", '(' to "\\(", ')' to "\\)",
            '[' to "\\[", ']' to "\\]", '.' to "\\.", '+' to "\\+", '*' to "\\*",
            '?' to "\\?", '^' to "\\^", '$' to "\\$", '|' to "\\|", '\\' to "\\\\"
        )
    }
}
