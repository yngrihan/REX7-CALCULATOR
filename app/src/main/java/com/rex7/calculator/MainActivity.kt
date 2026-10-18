package com.rex7.calculator

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var chassisLayout: LinearLayout
    private lateinit var lcdBorderLayout: LinearLayout
    private lateinit var lcdScreen: LinearLayout
    private lateinit var tvBrand: TextView
    private lateinit var tvLcdClock: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnHistory: TextView

    private val allButtons = mutableListOf<AppCompatButton>()
    private val currentInput = StringBuilder(128)
    private var isEvaluated = false
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable

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
        tvLcdClock = findViewById(R.id.tvLcdClock)
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        btnSettings = findViewById(R.id.btnSettings)
        btnHistory = findViewById(R.id.btnHistory)

        startLiveClock()
        collectAllButtons()
        bindKeyActions()
        setupClipboardControls()

        btnSettings.setOnClickListener { showSettingsDialog() }
        btnHistory.setOnClickListener { showHistoryDialog() }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTheme = prefs.getInt(KEY_THEME, 0)
        applyTheme(currentTheme)
    }

    private fun startLiveClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                tvLcdClock.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
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
            findViewById<AppCompatButton>(id)?.let {
                it.backgroundTintList = null
                allButtons.add(it)
            }
        }
    }

    private fun setupClipboardControls() {
        tvResult.setOnLongClickListener {
            val text = tvResult.text.toString()
            if (text.isNotEmpty() && text != "Error") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("REX7 Result", text))
                Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        tvExpression.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val raw = clip.getItemAt(0).coerceToText(this).toString()
                val sanitized = raw.replace("*", "×").replace("/", "÷").replace("-", "−")
                    .filter { it in "0123456789+−×÷%.() " }.trim()

                if (sanitized.isNotEmpty()) {
                    if (isEvaluated) {
                        currentInput.setLength(0)
                        isEvaluated = false
                    }
                    currentInput.append(sanitized)
                    updateLcdLive()
                    Toast.makeText(this, "Pasted", Toast.LENGTH_SHORT).show()
                }
                true
            } else false
        }
    }

    private fun makeBevel(baseColor: Int, topBorder: Int, bottomColor: Int, radiusDp: Float): StateListDrawable {
        val density = resources.displayMetrics.density
        val r = radiusDp * density
        fun createShape(color: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = r
                setColor(color)
                setStroke((2.2f * density).toInt(), topBorder)
            }
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), createShape(bottomColor))
            addState(intArrayOf(), createShape(baseColor))
        }
    }

    private fun applyTheme(themeIndex: Int) {
        val density = resources.displayMetrics.density
        val chassisRadius = 18 * density
        val lcdRadius = 12 * density

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
                    setColor(Color.parseColor("#8FA866"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val lcdText = Color.parseColor("#15220F")
                tvExpression.setTextColor(lcdText)
                tvResult.setTextColor(lcdText)
                tvLcdClock.setTextColor(lcdText)

                val burgundy = Color.parseColor("#6A2222")
                val burgundyPress = Color.parseColor("#421313")
                val burgundyLight = Color.parseColor("#8A2D2D")

                for (btn in allButtons) {
                    btn.background = makeBevel(burgundy, burgundyLight, burgundyPress, 12f)
                    btn.setTextColor(Color.parseColor("#F5EADB"))
                }
            }
            1 -> {
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#16181B"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#2A2E35"))
                }
                tvBrand.setTextColor(Color.WHITE)
                btnSettings.setTextColor(Color.WHITE)

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C0E10"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EAEFEA"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val darkLcdText = Color.parseColor("#111315")
                tvExpression.setTextColor(darkLcdText)
                tvResult.setTextColor(darkLcdText)
                tvLcdClock.setTextColor(darkLcdText)

                val darkBtn = Color.parseColor("#272A30")
                val darkBtnPress = Color.parseColor("#17191C")
                val darkBtnLight = Color.parseColor("#383C45")

                val redBtn = Color.parseColor("#BA2B2B")
                val redBtnPress = Color.parseColor("#7D1717")
                val redBtnLight = Color.parseColor("#DC3D3D")

                for (btn in allButtons) {
                    if (btn.id == R.id.btnAC || btn.id == R.id.btnEqual) {
                        btn.background = makeBevel(redBtn, redBtnLight, redBtnPress, 12f)
                        btn.setTextColor(Color.WHITE)
                    } else {
                        btn.background = makeBevel(darkBtn, darkBtnLight, darkBtnPress, 12f)
                        btn.setTextColor(Color.parseColor("#EAEFF5"))
                    }
                }
            }
            2 -> {
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#9B7EBD"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#B49BCE"))
                }
                tvBrand.setTextColor(Color.parseColor("#2B1540"))
                btnSettings.setTextColor(Color.parseColor("#2B1540"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#3C2454"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EFE7F7"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val purpleLcdText = Color.parseColor("#220F33")
                tvExpression.setTextColor(purpleLcdText)
                tvResult.setTextColor(purpleLcdText)
                tvLcdClock.setTextColor(purpleLcdText)

                val purpleBtn = Color.parseColor("#5A3E76")
                val purpleBtnPress = Color.parseColor("#3B2550")
                val purpleBtnLight = Color.parseColor("#77549A")
                val whiteKey = Color.parseColor("#F0E6F7")
                val whiteKeyPress = Color.parseColor("#CFBCE2")

                for (btn in allButtons) {
                    when (btn.id) {
                        R.id.btnAC, R.id.btnDel, R.id.btnToggleSign, R.id.btnPercent -> {
                            btn.background = makeBevel(purpleBtn, purpleBtnLight, purpleBtnPress, 12f)
                            btn.setTextColor(Color.WHITE)
                        }
                        else -> {
                            btn.background = makeBevel(whiteKey, Color.WHITE, whiteKeyPress, 12f)
                            btn.setTextColor(Color.parseColor("#2A143D"))
                        }
                    }
                }
            }
        }
        updateLcdLive()
    }

    private fun showSettingsDialog() {
        val themeNames = arrayOf("Classic Nokia (Green Screen)", "Dark Retro (White Screen)", "Purple Pixel (Lilac Screen)")
        AlertDialog.Builder(this)
            .setTitle("Select Calculator Theme")
            .setSingleChoiceItems(themeNames, currentTheme) { dialog, which ->
                currentTheme = which
                applyTheme(currentTheme)
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_THEME, currentTheme).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bindKeyActions() {
        val numberMap = listOf(
            R.id.btn0 to '0', R.id.btn1 to '1', R.id.btn2 to '2',
            R.id.btn3 to '3', R.id.btn4 to '4', R.id.btn5 to '5',
            R.id.btn6 to '6', R.id.btn7 to '7', R.id.btn8 to '8',
            R.id.btn9 to '9', R.id.btnDot to '.'
        )
        for ((id, ch) in numberMap) {
            findViewById<AppCompatButton>(id)?.setOnClickListener {
                if (isEvaluated) {
                    currentInput.setLength(0)
                    isEvaluated = false
                }
                currentInput.append(ch)
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnPlus)?.setOnClickListener { appendOperator('+') }
        findViewById<AppCompatButton>(R.id.btnMinus)?.setOnClickListener { appendOperator('−') }
        findViewById<AppCompatButton>(R.id.btnMultiply)?.setOnClickListener { appendOperator('×') }
        findViewById<AppCompatButton>(R.id.btnDivide)?.setOnClickListener { appendOperator('÷') }
        findViewById<AppCompatButton>(R.id.btnPercent)?.setOnClickListener { appendOperator('%') }
        findViewById<AppCompatButton>(R.id.btnParenOpen)?.setOnClickListener { 
            if (isEvaluated) { currentInput.setLength(0); isEvaluated = false }
            currentInput.append('(')
            updateLcdLive() 
        }
        findViewById<AppCompatButton>(R.id.btnParenClose)?.setOnClickListener { 
            currentInput.append(')')
            updateLcdLive() 
        }

        findViewById<AppCompatButton>(R.id.btnToggleSign)?.setOnClickListener {
            val s = currentInput.toString()
            if (s.isNotEmpty()) {
                if (s.startsWith("−")) {
                    currentInput.delete(0, 1)
                } else {
                    currentInput.insert(0, "−")
                }
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnAC)?.setOnClickListener {
            currentInput.setLength(0)
            isEvaluated = false
            tvExpression.text = ""
            tvResult.text = "0"
            measureAndScaleText(tvExpression, "", 42f, 18f)
            measureAndScaleText(tvResult, "0", 50f, 22f)
        }

        findViewById<AppCompatButton>(R.id.btnDel)?.setOnClickListener {
            if (isEvaluated) {
                currentInput.setLength(0)
                isEvaluated = false
                tvExpression.text = ""
                tvResult.text = "0"
                measureAndScaleText(tvExpression, "", 42f, 18f)
                measureAndScaleText(tvResult, "0", 50f, 22f)
            } else if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnEqual)?.setOnClickListener {
            if (currentInput.isEmpty()) return@setOnClickListener
            val expr = currentInput.toString()
            val ans = solveMath(expr)

            if (ans != null && ans != "Error") {
                tvExpression.text = expr
                tvResult.text = ans
                measureAndScaleText(tvExpression, expr, 42f, 18f)
                measureAndScaleText(tvResult, ans, 50f, 22f)

                currentInput.setLength(0)
                currentInput.append(ans)
                isEvaluated = true

                bgExecutor.execute { saveHistory(expr, ans) }
            } else {
                tvResult.text = "Error"
                measureAndScaleText(tvResult, "Error", 50f, 22f)
            }
        }
    }

    private fun appendOperator(op: Char) {
        if (isEvaluated) {
            isEvaluated = false
        }
        if (currentInput.isEmpty()) {
            if (op == '−') {
                currentInput.append('−')
                updateLcdLive()
            }
            return
        }
        val last = currentInput.last()
        if (last in "+−×÷%") {
            currentInput.setCharAt(currentInput.length - 1, op)
        } else {
            currentInput.append(op)
        }
        updateLcdLive()
    }

    private fun updateLcdLive() {
        val s = currentInput.toString()
        val displayText = if (s.isNotEmpty()) "$s|" else ""
        tvExpression.text = displayText
        measureAndScaleText(tvExpression, displayText, 42f, 18f)

        if (s.isNotEmpty()) {
            if (s == "." || s == "−") return
            val preview = solveMath(s)
            if (preview != null) {
                tvResult.text = preview
                measureAndScaleText(tvResult, preview, 50f, 22f)
            }
        } else {
            tvResult.text = "0"
            measureAndScaleText(tvResult, "0", 50f, 22f)
        }
    }

    private fun measureAndScaleText(textView: TextView, text: String, maxSp: Float, minSp: Float) {
        if (text.isEmpty()) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxSp)
            return
        }

        val availableWidth = (textView.width - textView.paddingStart - textView.paddingEnd).toFloat()
        if (availableWidth <= 0f) {
            textView.post { measureAndScaleText(textView, text, maxSp, minSp) }
            return
        }

        val paint = Paint(textView.paint)
        val density = resources.displayMetrics.scaledDensity

        var currentSp = maxSp
        while (currentSp > minSp) {
            paint.textSize = currentSp * density
            if (paint.measureText(text) <= availableWidth) {
                break
            }
            currentSp -= 1.5f
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSp)
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
                        tvExpression.text = ""
                        tvResult.text = part
                        isEvaluated = true
                        measureAndScaleText(tvExpression, "", 42f, 18f)
                        measureAndScaleText(tvResult, part, 50f, 22f)
                    }
                }
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear All") { _, _ -> bgExecutor.execute { prefs.edit().remove(KEY_HISTORY).apply() } }
            .show()
    }

    private fun solveMath(expr: String): String? {
        return try {
            val sanitized = expr.replace('×', '*').replace('÷', '/').replace('−', '-')
            val res = MathParser(sanitized).eval()
            if (res.isNaN() || res.isInfinite()) null else formatDecimal(res)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDecimal(value: Double): String {
        return try {
            val bd = BigDecimal(value.toString(), MathContext(12, RoundingMode.HALF_UP))
            val stripped = bd.stripTrailingZeros()
            stripped.toPlainString()
        } catch (_: Exception) {
            if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
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
            val v = parseAddSub()
            if (idx < src.length && c != '\u0000') return Double.NaN
            return v
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
                val sub = src.substring(start, idx)
                v = if (sub == "." || sub.isEmpty()) 0.0 else sub.toDouble()
            } else {
                return 0.0
            }
            if (eat('%')) v /= 100.0
            return v
        }
    }
}
