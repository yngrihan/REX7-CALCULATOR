package com.rex7.calculator

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var currentInput = StringBuilder()

    private val PREFS_NAME = "rex7_calculator_prefs"
    private val KEY_HISTORY = "history_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        findViewById<TextView>(R.id.btnHistory)?.setOnClickListener {
            showHistoryDialog()
        }

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
            saveToHistory("$expr = $res")
            tvExpression.text = expr
            tvResult.text = res
            currentInput = StringBuilder(res)
        } else {
            tvResult.text = "Error"
        }
    }

    private fun saveToHistory(entry: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_HISTORY, "") ?: ""
        val updated = if (existing.isEmpty()) entry else "$entry\n$existing"
        prefs.edit().putString(KEY_HISTORY, updated).apply()
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyRaw = prefs.getString(KEY_HISTORY, "") ?: ""
        val historyItems = if (historyRaw.isEmpty()) arrayOf("No history yet") else historyRaw.split("\n").filter { it.isNotBlank() }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Calculation History")
        builder.setItems(historyItems) { _, which ->
            if (historyRaw.isNotEmpty()) {
                val selected = historyItems[which]
                val resultPart = selected.substringAfterLast("=", "").trim()
                if (resultPart.isNotEmpty()) {
                    currentInput = StringBuilder(resultPart)
                    tvExpression.text = ""
                    tvResult.text = resultPart
                }
            }
        }
        builder.setPositiveButton("Close", null)
        builder.setNegativeButton("Clear All") { _, _ ->
            prefs.edit().remove(KEY_HISTORY).apply()
        }
        builder.show()
    }

    private fun evaluateExpression(expr: String): String? {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            val parser = MathParser(sanitized)
            val result = parser.parse()
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

    private class MathParser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            return parseExpr()
        }

        private fun parseExpr(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
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

        private fun parseFactor(): Double {
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
    }
}
