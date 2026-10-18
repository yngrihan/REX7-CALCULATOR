package com.rex7.calculator

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var chassisLayout: LinearLayout
    private lateinit var lcdBorderLayout: LinearLayout
    private lateinit var lcdScreen: LinearLayout
    private lateinit var tvBrand: TextView
    private lateinit var tvLcdFlag: TextView
    private lateinit var tvLcdClock: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnHistory: TextView

    private val allButtons = mutableListOf<Button>()
    private val currentInput = StringBuilder(64)
    private var lastCalculated = ""
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val PREFS_NAME = "rex7_calc_prefs"
    private val KEY_THEME = "selected_theme_index"
    private val KEY_HISTORY = "history_entries"

    private var currentTheme = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chassisLayout = findViewById(R.id.chassisLayout)
        lcdBorderLayout = findViewById(R.id.lcdBorderLayout)
        lcdScreen = findViewById(R.id.lcdScreen)
        tvBrand = findViewById(R.id.tvBrand)
        tvLcdFlag = findViewById(R.id.tvLcdFlag)
        tvLcdClock = findViewById(R.id.tvLcdClock)
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        btnSettings = findViewById(R.id.btnSettings)
        btnHistory = findViewById(R.id.btnHistory)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvLcdClock.text = timeFormat.format(Date())

        collectAllButtons()
        bindKeyActions()

        btnSettings.setOnClickListener { showSettingsDialog() }
        btnHistory.setOnClickListener { showHistoryDialog() }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTheme = prefs.getInt(KEY_THEME, 0)
        applyTheme(currentTheme)
    }

    private fun collectAllButtons() {
        val buttonIds = listOf(
            R.id.btnAC, R.id.btnDel, R.id.btnToggleSign, R.id.btnPercent,
            R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDivide,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btnMultiply,
            R.id.btn1, R.id.btn2, R.id.btn3, R.id.btnMinus,
            R.id.btnParenOpen, R.id.btn0, R.id.btnParenClose, R.id.btnPlus,
            R.id.btnDot, R.id.btnEqual
        )
        allButtons.clear()
        for (id in buttonIds) {
            findViewById<Button>(id)?.let { allButtons.add(it) }
        }
    }

    private fun makeBevelDrawable(baseColor: Int, topBorder: Int, bottomBorder: Int, radiusDp: Float): StateListDrawable {
        val density = resources.displayMetrics.density
        val r = radiusDp * density

        fun createShape(color: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = r
                setColor(color)
                setStroke((2 * density).toInt(), topBorder)
            }
        }

        val normal = createShape(baseColor)
        val pressed = createShape(bottomBorder)

        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun applyTheme(themeIndex: Int) {
        val density = resources.displayMetrics.density
        val chassisRadius = (18 * density)
        val lcdRadius = (12 * density)

        when (themeIndex) {
            0 -> {
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E4D7C2"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#C3B39B"))
                }
                tvBrand.setTextColor(Color.parseColor("#26211C"))
                btnSettings.setTextColor(Color.parseColor("#26211C"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2E2D28"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#8DA469"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val lcdText = Color.parseColor("#1B2313")
                tvExpression.setTextColor(lcdText)
                tvResult.setTextColor(lcdText)
                tvLcdFlag.setTextColor(lcdText)
                tvLcdClock.setTextColor(lcdText)

                val burgundy = Color.parseColor("#742525")
                val burgundyPressed = Color.parseColor("#531818")
                val burgundyLight = Color.parseColor("#8E3232")

                for (btn in allButtons) {
                    btn.background = makeBevelDrawable(burgundy, burgundyLight, burgundyPressed, 10f)
                    btn.setTextColor(Color.parseColor("#F5EADB"))
                }
            }
            1 -> {
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1F2124"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#383C42"))
                }
                tvBrand.setTextColor(Color.parseColor("#ECEFF1"))
                btnSettings.setTextColor(Color.parseColor("#ECEFF1"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#121315"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#87A868"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val lcdText = Color.parseColor("#142010")
                tvExpression.setTextColor(lcdText)
                tvResult.setTextColor(lcdText)
                tvLcdFlag.setTextColor(lcdText)
                tvLcdClock.setTextColor(lcdText)

                val darkBtn = Color.parseColor("#2B2E33")
                val darkBtnPress = Color.parseColor("#1B1C1F")
                val darkBtnLight = Color.parseColor("#3D4249")

                val redBtn = Color.parseColor("#A82D2D")
                val redBtnPress = Color.parseColor("#7A1C1C")
                val redBtnLight = Color.parseColor("#C73A3A")

                for (btn in allButtons) {
                    if (btn.id == R.id.btnAC || btn.id == R.id.btnEqual) {
                        btn.background = makeBevelDrawable(redBtn, redBtnLight, redBtnPress, 10f)
                    } else {
                        btn.background = makeBevelDrawable(darkBtn, darkBtnLight, darkBtnPress, 10f)
                    }
                    btn.setTextColor(Color.parseColor("#E0E6ED"))
                }
            }
            2 -> {
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#9B7EBD"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#B49BCE"))
                }
                tvBrand.setTextColor(Color.parseColor("#28163B"))
                btnSettings.setTextColor(Color.parseColor("#28163B"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#352349"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#8CA86D"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val lcdText = Color.parseColor("#1C2612")
                tvExpression.setTextColor(lcdText)
                tvResult.setTextColor(lcdText)
                tvLcdFlag.setTextColor(lcdText)
                tvLcdClock.setTextColor(lcdText)

                val purpleBtn = Color.parseColor("#5A3E76")
                val purpleBtnPress = Color.parseColor("#3F2856")
                val purpleBtnLight = Color.parseColor("#745396")

                val whiteKey = Color.parseColor("#EADEF7")
                val whiteKeyPress = Color.parseColor("#C9B7E0")
                val whiteKeyLight = Color.parseColor("#FFFFFF")

                for (btn in allButtons) {
                    val id = btn.id
                    if (id == R.id.btnAC || id == R.id.btnDel || id == R.id.btnToggleSign || id == R.id.btnPercent) {
                        btn.background = makeBevelDrawable(purpleBtn, purpleBtnLight, purpleBtnPress, 10f)
                        btn.setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        btn.background = makeBevelDrawable(whiteKey, whiteKeyLight, whiteKeyPress, 10f)
                        btn.setTextColor(Color.parseColor("#2E1945"))
                    }
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val themeNames = arrayOf(
            "Classic Nokia (Default)",
            "Dark Retro",
            "Purple Pixel"
        )
        AlertDialog.Builder(this)
            .setTitle("Select Calculator Theme")
            .setSingleChoiceItems(themeNames, currentTheme) { dialog, which ->
                currentTheme = which
                applyTheme(currentTheme)
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_THEME, currentTheme)
                    .apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindInstantTouch(id: Int, onAction: () -> Unit) {
        findViewById<View>(id)?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.isPressed = true
                onAction()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.isPressed = false
            }
            true
        }
    }

    private fun bindKeyActions() {
        val numberMap = listOf(
            R.id.btn0 to '0', R.id.btn1 to '1', R.id.btn2 to '2',
            R.id.btn3 to '3', R.id.btn4 to '4', R.id.btn5 to '5',
            R.id.btn6 to '6', R.id.btn7 to '7', R.id.btn8 to '8',
            R.id.btn9 to '9', R.id.btnDot to '.'
        )

        for ((id, ch) in numberMap) {
            bindInstantTouch(id) {
                currentInput.append(ch)
                updateLcdLive()
            }
        }

        bindInstantTouch(R.id.btnPlus) { appendOperator('+') }
        bindInstantTouch(R.id.btnMinus) { appendOperator('-') }
        bindInstantTouch(R.id.btnMultiply) { appendOperator('×') }
        bindInstantTouch(R.id.btnDivide) { appendOperator('÷') }
        bindInstantTouch(R.id.btnPercent) { appendOperator('%') }
        bindInstantTouch(R.id.btnParenOpen) { currentInput.append('('); updateLcdLive() }
        bindInstantTouch(R.id.btnParenClose) { currentInput.append(')'); updateLcdLive() }

        bindInstantTouch(R.id.btnToggleSign) {
            val s = currentInput.toString()
            if (s.isNotEmpty()) {
                if (s.startsWith("-")) {
                    currentInput.setLength(0)
                    currentInput.append(s.substring(1))
                } else {
                    currentInput.insert(0, "-")
                }
                updateLcdLive()
            }
        }

        bindInstantTouch(R.id.btnAC) {
            currentInput.setLength(0)
            lastCalculated = ""
            tvExpression.text = ""
            tvResult.text = "0"
        }

        bindInstantTouch(R.id.btnDel) {
            if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateLcdLive()
            }
        }

        bindInstantTouch(R.id.btnEqual) {
            if (currentInput.isEmpty()) return@bindInstantTouch
            val expr = currentInput.toString()
            val ans = if (lastCalculated.isNotEmpty()) lastCalculated else solveMath(expr)

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

    private fun appendOperator(op: Char) {
        if (currentInput.isEmpty()) {
            if (op == '-') {
                currentInput.append('-')
                updateLcdLive()
            }
            return
        }
        val last = currentInput.last()
        if (last in "+-×÷%") {
            currentInput.setCharAt(currentInput.length - 1, op)
        } else {
            currentInput.append(op)
        }
        updateLcdLive()
    }

    private fun updateLcdLive() {
        val s = currentInput.toString()
        tvExpression.text = s
        if (s.isNotEmpty()) {
            val preview = solveMath(s)
            if (preview != null) {
                lastCalculated = preview
                tvResult.text = preview
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
            val entry = "$expr = $ans"
            val updated = if (old.isEmpty()) entry else "$entry\n$old"
            prefs.edit().putString(KEY_HISTORY, updated).apply()
        } catch (_: Exception) {}
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        val items = if (raw.isEmpty()) arrayOf("No history yet") else raw.split("\n").filter { it.isNotBlank() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Calculation History")
            .setItems(items) { _, which ->
                if (raw.isNotEmpty()) {
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
            .setNegativeButton("Clear All") { _, _ ->
                bgExecutor.execute {
                    prefs.edit().remove(KEY_HISTORY).apply()
                }
            }
            .show()
    }

    private fun solveMath(expr: String): String? {
        return try {
            val sanitized = expr.replace('×', '*').replace('÷', '/')
            val res = MathParser(sanitized).eval()
            if (res.isNaN() || res.isInfinite()) return null
            if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.4f", res).trimEnd('0').trimEnd('.')
        } catch (_: Exception) {
            null
        }
    }

    private class MathParser(private val src: String) {
        private var idx = -1
        private var c = ' '

        private fun advance() {
            idx++
            c = if (idx < src.length) src[idx] else '\u0000'
        }

        private fun eat(target: Char): Boolean {
            while (c == ' ') advance()
            if (c == target) {
                advance()
                return true
            }
            return false
        }

        fun eval(): Double {
            advance()
            return parseAddSub()
        }

        private fun parseAddSub(): Double {
            var v = parseMulDiv()
            while (true) {
                when {
                    eat('+') -> v += parseMulDiv()
                    eat('-') -> v -= parseMulDiv()
                    else -> return v
                }
            }
        }

        private fun parseMulDiv(): Double {
            var v = parseFactor()
            while (true) {
                when {
                    eat('*') -> v *= parseFactor()
                    eat('/') -> {
                        val d = parseFactor()
                        v = if (d != 0.0) v / d else Double.NaN
                    }
                    else -> return v
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var v: Double
            val start = idx
            if (eat('(')) {
                v = parseAddSub()
                eat(')')
            } else if ((c in '0'..'9') || c == '.') {
                while ((c in '0'..'9') || c == '.') advance()
                v = src.substring(start, idx).toDouble()
            } else {
                return 0.0
            }
            if (eat('%')) v /= 100.0
            return v
        }
    }
}
