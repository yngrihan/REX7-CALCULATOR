package com.rex7.calculator

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private val currentInput = StringBuilder(64)
    private var lastCalculated = ""
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val PREFS_NAME = "rex7_calculator_prefs"
    private val KEY_HISTORY = "history_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        findViewById<View>(R.id.btnHistory)?.setOnClickListener {
            showHistoryDialog()
        }

        bindFastButtons()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindInstant(id: Int, onTouch: () -> Unit) {
        val v = findViewById<View>(id) ?: return
        v.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.isPressed = true
                onTouch()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                view.isPressed = false
            }
            true
        }
    }

    private fun bindFastButtons() {
        val digits = listOf(
            R.id.btn0 to '0', R.id.btn1 to '1', R.id.btn2 to '2',
            R.id.btn3 to '3', R.id.btn4 to '4', R.id.btn5 to '5',
            R.id.btn6 to '6', R.id.btn7 to '7', R.id.btn8 to '8',
            R.id.btn9 to '9', R.id.btnDot to '.'
        )

        for ((id, ch) in digits) {
            bindInstant(id) {
                currentInput.append(ch)
                renderLive()
            }
        }

        bindInstant(R.id.btnPlus) { appendOp('+') }
        bindInstant(R.id.btnMinus) { appendOp('-') }
        bindInstant(R.id.btnMultiply) { appendOp('×') }
        bindInstant(R.id.btnDivide) { appendOp('÷') }
        bindInstant(R.id.btnPercent) { appendOp('%') }

        bindInstant(R.id.btnParens) {
            var open = 0
            var close = 0
            for (i in 0 until currentInput.length) {
                if (currentInput[i] == '(') open++
                else if (currentInput[i] == ')') close++
            }
            if (open > close && currentInput.isNotEmpty() && (currentInput.last().isDigit() || currentInput.last() == ')')) {
                currentInput.append(')')
            } else {
                currentInput.append('(')
            }
            renderLive()
        }

        bindInstant(R.id.btnAC) {
            currentInput.setLength(0)
            lastCalculated = ""
            tvExpression.text = ""
            tvResult.text = "0"
        }

        bindInstant(R.id.btnDel) {
            if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                renderLive()
            }
        }

        // 0ms Pure Swap on Equals
        bindInstant(R.id.btnEqual) {
            if (currentInput.isEmpty()) return@bindInstant
            val expr = currentInput.toString()
            val ans = if (lastCalculated.isNotEmpty()) lastCalculated else quickSolve(expr)

            if (ans != null && ans != "Error") {
                tvExpression.text = expr
                tvResult.text = ans
                currentInput.setLength(0)
                currentInput.append(ans)
                lastCalculated = ans

                bgExecutor.execute {
                    saveHistory(expr, ans)
                }
            } else {
                tvResult.text = "Error"
            }
        }
    }

    private fun appendOp(op: Char) {
        if (currentInput.isEmpty()) {
            if (op == '-') {
                currentInput.append('-')
                renderLive()
            }
            return
        }
        val last = currentInput.last()
        if (last == '+' || last == '-' || last == '×' || last == '÷' || last == '%') {
            currentInput.setCharAt(currentInput.length - 1, op)
        } else {
            currentInput.append(op)
        }
        renderLive()
    }

    private fun renderLive() {
        val str = currentInput.toString()
        tvExpression.text = str
        if (str.isNotEmpty()) {
            val calc = quickSolve(str)
            if (calc != null) {
                lastCalculated = calc
                tvResult.text = calc
            }
        } else {
            lastCalculated = ""
            tvResult.text = "0"
        }
    }

    private fun saveHistory(expr: String, ans: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val old = prefs.getString(KEY_HISTORY, "") ?: ""
            val item = "$expr = $ans"
            val updated = if (old.isEmpty()) item else "$item\n$old"
            prefs.edit().putString(KEY_HISTORY, updated).apply()
        } catch (_: Exception) {}
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyRaw = prefs.getString(KEY_HISTORY, "") ?: ""
        val items = if (historyRaw.isEmpty()) arrayOf("No history yet") else historyRaw.split("\n").filter { it.isNotBlank() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("History")
            .setItems(items) { _, which ->
                if (historyRaw.isNotEmpty()) {
                    val sel = items[which]
                    val part = sel.substringAfterLast('=', "").trim()
                    if (part.isNotEmpty()) {
                        currentInput.setLength(0)
                        currentInput.append(part)
                        lastCalculated = part
                        tvExpression.text = ""
                        tvResult.text = part
                    }
                }
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear") { _, _ ->
                bgExecutor.execute {
                    prefs.edit().remove(KEY_HISTORY).apply()
                }
            }
            .show()
    }

    private fun quickSolve(expr: String): String? {
        return try {
            val clean = expr.replace('×', '*').replace('÷', '/')
            val res = Parser(clean).eval()
            if (res.isNaN() || res.isInfinite()) return null
            if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.4f", res).trimEnd('0').trimEnd('.')
        } catch (_: Exception) {
            null
        }
    }

    private class Parser(private val s: String) {
        private var idx = -1
        private var cur = ' '

        private fun advance() {
            idx++
            cur = if (idx < s.length) s[idx] else '\u0000'
        }

        private fun match(c: Char): Boolean {
            while (cur == ' ') advance()
            if (cur == c) {
                advance()
                return true
            }
            return false
        }

        fun eval(): Double {
            advance()
            return parseSum()
        }

        private fun parseSum(): Double {
            var v = parseMul()
            while (true) {
                when {
                    match('+') -> v += parseMul()
                    match('-') -> v -= parseMul()
                    else -> return v
                }
            }
        }

        private fun parseMul(): Double {
            var v = parsePrimary()
            while (true) {
                when {
                    match('*') -> v *= parsePrimary()
                    match('/') -> {
                        val d = parsePrimary()
                        v = if (d != 0.0) v / d else Double.NaN
                    }
                    else -> return v
                }
            }
        }

        private fun parsePrimary(): Double {
            if (match('+')) return parsePrimary()
            if (match('-')) return -parsePrimary()

            var v: Double
            val start = idx
            if (match('(')) {
                v = parseSum()
                match(')')
            } else if ((cur in '0'..'9') || cur == '.') {
                while ((cur in '0'..'9') || cur == '.') advance()
                v = s.substring(start, idx).toDouble()
            } else {
                return 0.0
            }
            if (match('%')) v /= 100.0
            return v
        }
    }
}
