package com.rex7.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CalculationEngine {
    private val MC = MathContext(34, RoundingMode.HALF_EVEN)

    fun evaluate(rawExpr: String, isLivePreview: Boolean = false): String? {
        if (rawExpr.isBlank()) return null
        val prepared = preprocess(rawExpr, isLivePreview) ?: return null
        return try {
            val parser = BigMathParser(prepared)
            val result = parser.parse()
            formatResult(result)
        } catch (_: Exception) {
            null
        }
    }

    private fun preprocess(expr: String, isLivePreview: Boolean): String? {
        var s = expr.replace('×', '*').replace('÷', '/').replace('−', '-').replace(" ", "")
        if (s.isEmpty()) return null

        // Strip trailing incomplete operators/dots for safe live preview
        while (s.isNotEmpty() && (s.last() in "+-*/." || s.last() == '%')) {
            s = s.dropLast(1)
        }
        if (s.isEmpty()) return null

        // Balance open parentheses
        var balance = 0
        for (ch in s) {
            if (ch == '(') balance++
            else if (ch == ')') balance--
        }
        if (balance < 0) return null
        if (isLivePreview && balance > 0) {
            s += ")".repeat(balance)
        } else if (!isLivePreview && balance > 0) {
            s += ")".repeat(balance)
        }

        // Implicit multiplication: 7(8)6 -> 7*(8)*6, 2(3+4) -> 2*(3+4), (5)(5) -> (5)*(5)
        val sb = StringBuilder()
        for (i in 0 until s.length) {
            val curr = s[i]
            sb.append(curr)
            if (i < s.length - 1) {
                val next = s[i + 1]
                val currIsDigitOrDot = curr.isDigit() || curr == '.'
                val nextIsDigitOrDot = next.isDigit() || next == '.'
                if ((currIsDigitOrDot && next == '(') ||
                    (curr == ')' && next == '(') ||
                    (curr == ')' && nextIsDigitOrDot) ||
                    (curr == '%' && (nextIsDigitOrDot || next == '('))
                ) {
                    sb.append('*')
                }
            }
        }
        return sb.toString()
    }

    private fun formatResult(bd: BigDecimal): String {
        val stripped = bd.stripTrailingZeros()
        val plain = stripped.toPlainString()
        val absVal = stripped.abs()

        val needsSci = if (stripped.compareTo(BigDecimal.ZERO) == 0) false
        else (absVal >= BigDecimal("1e15") || (absVal < BigDecimal("1e-6") && absVal > BigDecimal.ZERO))

        return if (needsSci) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("0.######E0", symbols)
            df.format(stripped)
        } else {
            plain
        }
    }

    private class BigMathParser(private val src: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < src.length) src[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): BigDecimal {
            nextChar()
            val x = parseExpression()
            if (pos < src.length && ch != '\u0000') throw RuntimeException("Unexpected char")
            return x
        }

        private fun parseExpression(): BigDecimal {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x = x.add(parseTerm(), MC)
                    eat('-') -> x = x.subtract(parseTerm(), MC)
                    else -> return x
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x = x.multiply(parseFactor(), MC)
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                        x = x.divide(divisor, MC)
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): BigDecimal {
            if (eat('+')) return parseFactor()
            if (eat('-')) return parseFactor().negate()

            var x: BigDecimal
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                val token = src.substring(startPos, pos)
                if (token == "." || token.isEmpty()) return BigDecimal.ZERO
                x = BigDecimal(token, MC)
            } else {
                return BigDecimal.ZERO
            }

            if (eat('%')) {
                x = x.divide(BigDecimal("100"), MC)
            }
            return x
        }
    }
}
