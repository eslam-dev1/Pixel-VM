package com.example

import com.example.simulated.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculatorLogicTest {

    private lateinit var engine: CalculatorEngine

    @Before
    fun setUp() {
        engine = CalculatorEngine()
    }

    @Test
    fun testBasicAddition() {
        engine.inputDigit("5")
        engine.inputOperator("+")
        engine.inputDigit("3")
        val result = engine.calculate()
        assertEquals("8", result)
    }

    @Test
    fun testOperatorPrecedence() {
        // 2 + 3 * 4 should be 14, not 20
        engine.inputDigit("2")
        engine.inputOperator("+")
        engine.inputDigit("3")
        engine.inputOperator("×")
        engine.inputDigit("4")
        val result = engine.calculate()
        assertEquals("14", result)
    }

    @Test
    fun testParenthesisPrecedence() {
        // (2 + 3) * 4 should be 20
        engine.inputParenthesis() // inserts '('
        engine.inputDigit("2")
        engine.inputOperator("+")
        engine.inputDigit("3")
        engine.inputParenthesis() // inserts ')'
        engine.inputOperator("×")
        engine.inputDigit("4")
        val result = engine.calculate()
        assertEquals("20", result)
    }

    @Test
    fun testDivisionAndDecimals() {
        engine.inputDigit("7")
        engine.inputOperator("÷")
        engine.inputDigit("2")
        val result = engine.calculate()
        assertEquals("3.5", result)
    }

    @Test
    fun testBackspaceAndClear() {
        engine.inputDigit("1")
        engine.inputDigit("2")
        engine.inputDigit("3")
        engine.backspace()
        assertEquals("12", engine.expression)

        engine.clear()
        assertEquals("", engine.expression)
    }

    @Test
    fun testCalculationHistory() {
        engine.inputDigit("10")
        engine.inputOperator("+")
        engine.inputDigit("5")
        engine.calculate()

        assertEquals(1, engine.history.size)
        assertEquals("10+5", engine.history[0].expression)
        assertEquals("15", engine.history[0].result)
    }
}
