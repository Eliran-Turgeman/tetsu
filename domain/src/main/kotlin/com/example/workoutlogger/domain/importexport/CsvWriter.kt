package com.example.workoutlogger.domain.importexport

import java.io.Appendable

internal class CsvWriter(
    private val appendable: Appendable,
    private val delimiter: Char = ','
) {
    fun writeRow(fields: List<String>) {
        fields.forEachIndexed { index, field ->
            if (index > 0) {
                appendable.append(delimiter)
            }
            appendable.append(escape(field))
        }
        appendable.append('\n')
    }

    private fun escape(value: String): CharSequence {
        if (value.isEmpty()) return ""
        val needsQuote = value.any { it == '\n' || it == '\r' || it == '"' || it == delimiter }
        if (!needsQuote) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
