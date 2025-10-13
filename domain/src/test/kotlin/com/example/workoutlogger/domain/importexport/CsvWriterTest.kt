package com.example.workoutlogger.domain.importexport

import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvWriterTest {

    @Test
    fun `writer escapes quotes and commas`() {
        val writer = StringWriter()
        val csvWriter = CsvWriter(writer)
        csvWriter.writeRow(listOf("text", "comma,value", "quote\"value"))
        assertEquals("text,\"comma,value\",\"quote\"\"value\"\n", writer.toString())
    }
}
