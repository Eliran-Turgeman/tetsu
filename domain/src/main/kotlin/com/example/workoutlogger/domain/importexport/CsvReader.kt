package com.example.workoutlogger.domain.importexport

import java.io.IOException
import java.io.PushbackReader
import java.io.Reader

internal class CsvReader(
    reader: Reader,
    private val delimiter: Char
) : Sequence<List<String>> {

    private val input = PushbackReader(reader, 1)
    private var isFirstChar = true

    override fun iterator(): Iterator<List<String>> = object : Iterator<List<String>> {
        private var nextRow: List<String>? = readRow()

        override fun hasNext(): Boolean = nextRow != null

        override fun next(): List<String> {
            val row = nextRow ?: throw NoSuchElementException()
            nextRow = readRow()
            return row
        }
    }

    private fun readRow(): List<String>? {
        val row = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        while (true) {
            val intChar = input.read()
            if (intChar == -1) {
                if (inQuotes) {
                    throw IOException("Unterminated quoted field")
                }
                if (current.isEmpty() && row.isEmpty()) {
                    return null
                }
                row += current.toString()
                return row
            }
            val ch = intChar.toChar()
            if (isFirstChar) {
                isFirstChar = false
                if (ch == '\uFEFF') {
                    continue
                }
            }
            when {
                inQuotes -> {
                    when (ch) {
                        '"' -> {
                            val next = input.read()
                            if (next == '"'.code) {
                                current.append('"')
                            } else {
                                inQuotes = false
                                if (next != -1) {
                                    input.unread(next)
                                }
                            }
                        }

                        else -> current.append(ch)
                    }
                }

                ch == '"' -> inQuotes = true

                ch == delimiter -> {
                    row += current.toString()
                    current.clear()
                }

                ch == '\r' -> {
                    val next = input.read()
                    if (next != '\n'.code && next != -1) {
                        input.unread(next)
                    }
                    row += current.toString()
                    return row
                }

                ch == '\n' -> {
                    row += current.toString()
                    return row
                }

                else -> current.append(ch)
            }
        }
    }
}
