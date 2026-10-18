package com.rex7.calculator

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.HorizontalScrollView
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
    private lateinit var scrollExpression: HorizontalScrollView
    private lateinit var tvBrand: TextView
    private lateinit var tvLcdClock: TextView
    private lateinit var tvExpression: TextView
    private lateinit var cursorBar: View
    private lateinit var tvResult: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnHistory: TextView

    private val allButtons = mutableListOf<AppCompatButton>()
    private val currentInput = StringBuilder(128)
    private var isEvaluated = false
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var cursorVisible = true
    private lateinit var cursorRunnable: Runnable
    private lateinit var clockRunnable: Runnable

    private val PREFS_NAME = "rex7_calc_prefs"
    private val KEY_THEME = "selected_theme_index"
    private val KEY_HISTORY = "history_entries"
    private val KEY_SOUND_ENABLED = "sound_enabled"
    private val KEY_SOUND_INDEX = "sound_index"

    private var currentTheme = 0
    private var isSoundEnabled = true
    private var currentSoundIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chassisLayout = findViewById(R.id.chassisLayout)
        lcdBorderLayout = findViewById(R.id.lcdBorderLayout)
        lcdScreen = findViewById(R.id.lcdScreen)
        scrollExpression = findViewById(R.id.scrollExpression)
        tvBrand = findViewById(R.id.tvBrand)
        tvLcdClock = findViewById(R.id.tvLcdClock)
        tvExpression = findViewById(R.id.tvExpression)
        cursorBar = findViewById(R.id.cursorBar)
        tvResult = findViewById(R.id.tvResult)
        btnSettings = findViewById(R.id.btnSettings)
        btnHistory = findViewById(R.id.btnHistory)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTheme = prefs.getInt(KEY_THEME, 0)
        isSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        currentSoundIndex = prefs.getInt(KEY_SOUND_INDEX, 0)

        startClockAndCursor()
        collectAllButtons()
        bindKeyActions()
        setupDirectCopyPaste()

        btnSettings.setOnClickListener { showSettingsDialog() }
        btnHistory.setOnClickListener { showHistoryDialog() }

        applyTheme(currentTheme)
    }

    private fun startClockAndCursor() {
        clockRunnable = object : Runnable {
            override fun run() {
                tvLcdClock.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.post(clockRunnable)

        cursorRunnable = object : Runnable {
            override fun run() {
                if (!isEvaluated && currentInput.isNotEmpty()) {
                    cursorVisible = !cursorVisible
                    cursorBar.visibility = if (cursorVisible) View.VISIBLE else View.INVISIBLE
                } else {
                    cursorBar.visibility = View.GONE
                }
                mainHandler.postDelayed(this, 500)
            }
        }
        mainHandler.post(cursorRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(clockRunnable)
        mainHandler.removeCallbacks(cursorRunnable)
    }

    private fun triggerSound(view: View) {
        SoundManager.play(this, view, currentSoundIndex, isSoundEnabled)
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

    private fun setupDirectCopyPaste() {
        tvExpression.setOnClickListener {
            val text = tvExpression.text.toString().trim()
            if (isEvaluated && text.isNotEmpty() && text != "Error") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("REX7 Result", text))
                Toast.makeText(this, "Copied: $text", Toast.LENGTH_SHORT).show()
            }
        }

        lcdScreen.setOnLongClickListener {
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
                cursorBar.setBackgroundColor(lcdText)
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
                cursorBar.setBackgroundColor(darkLcdText)
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
                cursorBar.setBackgroundColor(purpleLcdText)
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
        val soundState = if (isSoundEnabled) "ON" else "OFF"
        val soundNames = arrayOf(
            "1. Standard Tap",
            "2. Mechanical Click",
            "3. Retro Nokia Beep",
            "4. Soft Bubble Pop",
            "5. Typewriter Clack",
            "6. Digital Bip",
            "7. Subtle Wood Tap"
        )
        val options = arrayOf(
            "🎨 Change Theme",
            "🔊 Keypad Sound: $soundState",
            "🎵 Select Sound Effect (${soundNames[currentSoundIndex]})"
        )

        AlertDialog.Builder(this)
            .setTitle("REX7 Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> {
                        isSoundEnabled = !isSoundEnabled
                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_SOUND_ENABLED, isSoundEnabled).apply()
                        val state = if (isSoundEnabled) "Sound Turned ON" else "Sound Muted"
                        Toast.makeText(this, state, Toast.LENGTH_SHORT).show()
                    }
                    2 -> showSoundSelectionDialog(soundNames)
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showThemeDialog() {
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

    private fun showSoundSelectionDialog(soundNames: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("Select Keypad Sound")
            .setSingleChoiceItems(soundNames, currentSoundIndex) { dialog, which ->
                currentSoundIndex = which
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_SOUND_INDEX, currentSoundIndex).apply()
                SoundManager.play(this, tvExpression, currentSoundIndex, true)
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
            findViewById<AppCompatButton>(id)?.setOnClickListener { v ->
                triggerSound(v)
                if (isEvaluated) {
                    currentInput.setLength(0)
                    isEvaluated = false
                }
                currentInput.append(ch)
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnPlus)?.setOnClickListener { v -> triggerSound(v); appendOperator('+') }
        findViewById<AppCompatButton>(R.id.btnMinus)?.setOnClickListener { v -> triggerSound(v); appendOperator('−') }
        findViewById<AppCompatButton>(R.id.btnMultiply)?.setOnClickListener { v -> triggerSound(v); appendOperator('×') }
        findViewById<AppCompatButton>(R.id.btnDivide)?.setOnClickListener { v -> triggerSound(v); appendOperator('÷') }
        findViewById<AppCompatButton>(R.id.btnPercent)?.setOnClickListener { v -> triggerSound(v); appendOperator('%') }
        findViewById<AppCompatButton>(R.id.btnParenOpen)?.setOnClickListener { v ->
            triggerSound(v)
            if (isEvaluated) { currentInput.setLength(0); isEvaluated = false }
            currentInput.append('(')
            updateLcdLive() 
        }
        findViewById<AppCompatButton>(R.id.btnParenClose)?.setOnClickListener { v ->
            triggerSound(v)
            currentInput.append(')')
            updateLcdLive() 
        }

        findViewById<AppCompatButton>(R.id.btnToggleSign)?.setOnClickListener { v ->
            triggerSound(v)
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

        findViewById<AppCompatButton>(R.id.btnAC)?.setOnClickListener { v ->
            triggerSound(v)
            currentInput.setLength(0)
            isEvaluated = false
            tvExpression.text = ""
            cursorBar.visibility = View.GONE
            tvResult.text = ""
            scaleDynamic(tvExpression, "", 48f)
        }

        findViewById<AppCompatButton>(R.id.btnDel)?.setOnClickListener { v ->
            triggerSound(v)
            if (isEvaluated) {
                currentInput.setLength(0)
                isEvaluated = false
                tvExpression.text = ""
                cursorBar.visibility = View.GONE
                tvResult.text = ""
                scaleDynamic(tvExpression, "", 48f)
            } else if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnEqual)?.setOnClickListener { v ->
            triggerSound(v)
            if (currentInput.isEmpty()) return@setOnClickListener
            val expr = currentInput.toString()
            val ans = solveMath(expr)

            if (ans != null && ans != "Error") {
                tvExpression.text = ans
                cursorBar.visibility = View.GONE
                tvResult.text = ""
                scaleDynamic(tvExpression, ans, 52f)

                currentInput.setLength(0)
                currentInput.append(ans)
                isEvaluated = true

                bgExecutor.execute { saveHistory(expr, ans) }
            } else {
                tvExpression.text = "Error"
                cursorBar.visibility = View.GONE
                tvResult.text = ""
                scaleDynamic(tvExpression, "Error", 48f)
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
        if (s.isEmpty()) {
            tvExpression.text = ""
            cursorBar.visibility = View.GONE
            tvResult.text = ""
            return
        }

        tvExpression.text = s
        cursorBar.visibility = View.VISIBLE
        scaleDynamic(tvExpression, s, 48f)

        scrollExpression.post { scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }

        val preview = solveMath(s)
        if (preview != null && preview != s) {
            tvResult.text = preview
            tvResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            tvResult.alpha = 0.65f
        } else {
            tvResult.text = ""
        }
    }

    private fun scaleDynamic(textView: TextView, text: String, maxSp: Float) {
        val len = text.length
        val calculatedSp = when {
            len <= 5 -> maxSp
            len <= 9 -> maxSp - 6f
            len <= 14 -> maxSp - 12f
            len <= 19 -> maxSp - 18f
            len <= 25 -> maxSp - 24f
            else -> 22f
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, calculatedSp)
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
                        tvExpression.text = part
                        cursorBar.visibility = View.GONE
                        tvResult.text = ""
                        isEvaluated = true
                        scaleDynamic(tvExpression, part, 52f)
                    }
                }
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear All") { _, _ -> bgExecutor.execute { prefs.edit().remove(KEY_HISTORY).apply() } }
            .show()
    }

    private fun solveMath(expr: String): String? {
        return try {
            var sanitized = expr.replace('×', '*').replace('÷', '/').replace('−', '-')
            while (sanitized.isNotEmpty() && (sanitized.last() in "+-*/." || sanitized.last() == '%')) {
                sanitized = sanitized.dropLast(1)
            }
            if (sanitized.isEmpty()) return null

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
