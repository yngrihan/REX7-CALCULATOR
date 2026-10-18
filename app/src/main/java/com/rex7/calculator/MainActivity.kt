package com.rex7.calculator

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var currentInput = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        setupButtons()
    }

    private fun setupButtons() {
        val numberButtons = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )

        for ((id, value) in numberButtons) {
            findViewById<AppCompatButton>(id)?.setOnClickListener {
                currentInput.append(value)
                updateDisplay()
            }
        }

        findViewById<AppCompatButton>(R.id.btnPlus)?.setOnClickListener { appendOperator("+") }
        findViewById<AppCompatButton>(R.id.btnMinus)?.setOnClickListener { appendOperator("-") }
        findViewById<AppCompatButton>(R.id.btnMultiply)?.setOnClickListener { appendOperator("×") }
        findViewById<AppCompatButton>(R.id.btnDivide)?.setOnClickListener { appendOperator("÷") }
        findViewById<AppCompatButton>(R.id.btnPercent)?.setOnClickListener { appendOperator("%") }

        findViewById<AppCompatButton>(R.id.btnParens)?.setOnClickListener {
            val openCount = currentInput.count { it == '(' }
            val closeCount = currentInput.count { it == ')' }
            if (openCount > closeCount && currentInput.isNotEmpty() && (currentInput.last().isDigit() || currentInput.last() == ')')) {
                currentInput.append(")")
            } else {
                currentInput.append("(")
            }
            updateDisplay()
        }

        findViewById<AppCompatButton>(R.id.btnAC)?.setOnClickListener {
            currentInput.clear()
            tvExpression.text = ""
            tvResult.text = "0"
        }

        findViewById<AppCompatButton>(R.id.btnDel)?.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateDisplay()
            }
        }

        findViewById<AppCompatButton>(R.id.btnEqual)?.setOnClickListener {
            calculateFinalResult()
        }
    }

    private fun appendOperator(op: String) {
        if (currentInput.isEmpty() && op == "-") {
            currentInput.append("-")
            updateDisplay()
            return
        }
        if (currentInput.isNotEmpty()) {
            val lastChar = currentInput.last()
            if (lastChar in "+-×÷%") {
                currentInput.setCharAt(currentInput.length - 1, op[0])
            } else {
                currentInput.append(op)
            }
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val expr = currentInput.toString()
        tvExpression.text = expr
        if (expr.isNotEmpty()) {
            val preview = evaluateExpression(expr)
            if (preview != null) {
                tvResult.text = preview
            }
        } else {
            tvResult.text = "0"
        }
    }

    private fun calculateFinalResult() {
        val expr = currentInput.toString()
        if (expr.isEmpty()) return
        val res = evaluateExpression(expr)
        if (res != null) {
            tvExpression.text = expr
            tvResult.text = res
            currentInput = StringBuilder(res)
        } else {
            tvResult.text = "Error"
        }
    }

    private fun evaluateExpression(expr: String): String? {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            val result = simpleEval(sanitized)
            if (result.isNaN() || result.isInfinite()) return null
            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format("%.4f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun simpleEval(str: String): Double {
        var pos = -1
        var ch = ' '

        fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpr()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                return 0.0
            }
            if (eat('%')) x /= 100.0
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        x = if (divisor != 0.0) x / divisor else Double.NaN
                    }
                    else -> return x
                }
            }
        }

        fun parseExpr(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        nextChar()
        return parseExpr()
    }
}
