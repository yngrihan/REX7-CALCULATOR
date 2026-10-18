package com.rex7.calculator

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var currentInput = StringBuilder()
    private var lastValidResult: String = ""
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val PREFS_NAME = "rex7_calculator_prefs"
    private val KEY_HISTORY = "history_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        findViewById<TextView>(R.id.btnHistory)?.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
            findViewById<AppCompatButton>(id)?.let { btn ->
                btn.setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    currentInput.append(value)
                    updateDisplay()
                }
            }
        }

        findViewById<AppCompatButton>(R.id.btnPlus)?.setOnClickListener { handleOp(it, "+") }
        findViewById<AppCompatButton>(R.id.btnMinus)?.setOnClickListener { handleOp(it, "-") }
        findViewById<AppCompatButton>(R.id.btnMultiply)?.setOnClickListener { handleOp(it, "×") }
        findViewById<AppCompatButton>(R.id.btnDivide)?.setOnClickListener { handleOp(it, "÷") }
        findViewById<AppCompatButton>(R.id.btnPercent)?.setOnClickListener { handleOp(it, "%") }

        findViewById<AppCompatButton>(R.id.btnParens)?.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val openCount = currentInput.count { it == '(' }
            val closeCount = currentInput.count { it == ')' }
            if (openCount > closeCount && currentInput.isNotEmpty() && (currentInput.last().isDigit() || currentInput.last() == ')')) {
                currentInput.append(")")
            } else {
                currentInput.append("(")
            }
            updateDisplay()
        }

        findViewById<AppCompatButton>(R.id.btnAC)?.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            currentInput.clear()
            lastValidResult = ""
            tvExpression.text = ""
            tvResult.text = "0"
        }

        findViewById<AppCompatButton>(R.id.btnDel)?.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateDisplay()
            }
        }

        // Instant Zero-Delay Equals Button
        findViewById<AppCompatButton>(R.id.btnEqual)?.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            calculateFinalResultInstant()
        }
    }

    private fun handleOp(v: View, op: String) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        appendOperator(op)
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
                lastValidResult = preview
                tvResult.text = preview
            }
        } else {
            lastValidResult = ""
            tvResult.text = "0"
        }
    }

    private fun calculateFinalResultInstant() {
        val expr = currentInput.toString()
        if (expr.isEmpty()) return

        // Agar result preview ready hai toh instantly use karo, recalculate me time waste mat karo
        val finalRes = if (lastValidResult.isNotEmpty()) lastValidResult else evaluateExpression(expr)

        if (finalRes != null && finalRes != "Error") {
            tvExpression.text = expr
            tvResult.text = finalRes
            currentInput = StringBuilder(finalRes)
            lastValidResult = finalRes

            // History background thread me save hogi taaki UI ek millisecond bhi na ruke
            bgExecutor.execute {
                saveToHistory("$expr = $finalRes")
            }
        } else {
            tvResult.text = "Error"
        }
    }

    private fun saveToHistory(entry: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_HISTORY, "") ?: ""
            val updated = if (existing.isEmpty()) entry else "$entry\n$existing"
            prefs.edit().putString(KEY_HISTORY, updated).apply()
        } catch (_: Exception) {}
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyRaw = prefs.getString(KEY_HISTORY, "") ?: ""
        val historyItems = if (historyRaw.isEmpty()) arrayOf("No history yet") else historyRaw.split("\n").filter { it.isNotBlank() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Calculation History")
            .setItems(historyItems) { _, which ->
                if (historyRaw.isNotEmpty()) {
                    val selected = historyItems[which]
                    val resultPart = selected.substringAfterLast("=", "").trim()
                    if (resultPart.isNotEmpty()) {
                        currentInput = StringBuilder(resultPart)
                        lastValidResult = resultPart
                        tvExpression.text = ""
                        tvResult.text = resultPart
                    }
                }
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear All") { _, _ ->
                bgExecutor.execute {
                    prefs.edit().remove(KEY_HISTORY).apply()
                }
            }
            .show()
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
