package com.example.simulated.calculator

class CalculatorEngine {

    data class HistoryItem(
        val expression: String,
        val result: String
    )

    var expression: String = ""
        private set

    var resultPreview: String = ""
        private set

    val history = mutableListOf<HistoryItem>()

    fun inputDigit(digit: String) {
        expression += digit
        computePreview()
    }

    fun inputOperator(op: String) {
        if (expression.isEmpty()) {
            if (op == "-") {
                expression = "-"
            }
            return
        }
        val lastChar = expression.last()
        if (lastChar in "+-×÷*/") {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        computePreview()
    }

    fun inputDecimal() {
        if (expression.isEmpty()) {
            expression = "0."
            computePreview()
            return
        }
        // Check if current number segment already has decimal
        val currentSegment = expression.split(Regex("[+\\-×÷*/()]")).lastOrNull() ?: ""
        if (!currentSegment.contains(".")) {
            expression += "."
            computePreview()
        }
    }

    fun inputParenthesis() {
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val lastChar = expression.lastOrNull()

        if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
            expression += ")"
        } else {
            if (lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
                expression += "×("
            } else {
                expression += "("
            }
        }
        computePreview()
    }

    fun toggleSign() {
        if (expression.isEmpty()) return
        val regex = Regex("([+\\-×÷*/(])?([0-9.]+)$")
        val match = regex.find(expression)
        if (match != null) {
            val prefix = expression.substring(0, match.range.first)
            val op = match.groupValues[1]
            val num = match.groupValues[2]
            val toggled = when (op) {
                "-" -> "+" + num
                "+" -> "-" + num
                "" -> if (prefix.endsWith("-")) num else "-" + num
                else -> "$op(-$num)"
            }
            expression = prefix + toggled
            computePreview()
        }
    }

    fun clear() {
        expression = ""
        resultPreview = ""
    }

    fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            computePreview()
        }
    }

    fun calculate(): String {
        if (expression.isBlank()) return ""
        val sanitized = expression.replace("×", "*").replace("÷", "/")
        return try {
            val eval = evaluateExpression(sanitized)
            val formatted = formatResult(eval)
            history.add(0, HistoryItem(expression, formatted))
            expression = formatted
            resultPreview = ""
            formatted
        } catch (_: Exception) {
            "Error"
        }
    }

    private fun computePreview() {
        if (expression.isBlank()) {
            resultPreview = ""
            return
        }
        val sanitized = expression.replace("×", "*").replace("÷", "/")
        try {
            val eval = evaluateExpression(sanitized)
            resultPreview = formatResult(eval)
        } catch (_: Exception) {
            resultPreview = ""
        }
    }

    companion object {
        fun evaluate(expr: String): Double {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            return ExpressionParser(sanitized).parse()
        }

        fun formatResult(value: Double): String {
            if (value.isNaN() || value.isInfinite()) return "Error"
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                val s = String.format(java.util.Locale.US, "%.8f", value).trimEnd('0')
                if (s.endsWith(".")) s.dropLast(1) else s
            }
        }

        private fun evaluateExpression(expr: String): Double {
            return ExpressionParser(expr).parse()
        }

        private class ExpressionParser(private val expr: String) {
            private var index = 0

            fun parse(): Double {
                val result = parseExpression()
                return result
            }

            private fun parseExpression(): Double {
                var result = parseTerm()
                while (index < expr.length) {
                    skipWhitespace()
                    if (index >= expr.length) break
                    val op = expr[index]
                    if (op == '+' || op == '-') {
                        index++
                        val next = parseTerm()
                        result = if (op == '+') result + next else result - next
                    } else {
                        break
                    }
                }
                return result
            }

            private fun parseTerm(): Double {
                var factor = parseFactor()
                while (index < expr.length) {
                    skipWhitespace()
                    if (index >= expr.length) break
                    val op = expr[index]
                    if (op == '*' || op == '/' || op == '%') {
                        index++
                        val next = parseFactor()
                        factor = when (op) {
                            '*' -> factor * next
                            '/' -> if (next == 0.0) throw ArithmeticException("Divide by zero") else factor / next
                            '%' -> factor % next
                            else -> factor
                        }
                    } else {
                        break
                    }
                }
                return factor
            }

            private fun parseFactor(): Double {
                skipWhitespace()
                if (index >= expr.length) return 0.0

                if (expr[index] == '+') {
                    index++
                    return parseFactor()
                }
                if (expr[index] == '-') {
                    index++
                    return -parseFactor()
                }

                if (expr[index] == '(') {
                    index++ // skip '('
                    val result = parseExpression()
                    skipWhitespace()
                    if (index < expr.length && expr[index] == ')') {
                        index++ // skip ')'
                    }
                    return result
                }

                val start = index
                while (index < expr.length && (expr[index].isDigit() || expr[index] == '.')) {
                    index++
                }
                val token = expr.substring(start, index)
                return token.toDoubleOrNull() ?: 0.0
            }

            private fun skipWhitespace() {
                while (index < expr.length && expr[index] == ' ') index++
            }
        }
    }
}
