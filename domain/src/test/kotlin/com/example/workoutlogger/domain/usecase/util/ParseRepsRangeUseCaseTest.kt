package com.example.workoutlogger.domain.usecase.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseRepsRangeUseCaseTest {

    private val useCase = ParseRepsRangeUseCase()

    @Test
    fun `parses single value`() {
        val result = useCase("8")
        assertEquals(8 to 8, result)
    }

    @Test
    fun `parses range with spaces`() {
        val result = useCase(" 6 - 10 ")
        assertEquals(6 to 10, result)
    }

    @Test
    fun `returns null for invalid range`() {
        assertNull(useCase("ten"))
        assertNull(useCase("8-2"))
        assertNull(useCase(""))
    }
}
