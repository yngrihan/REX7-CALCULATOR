package com.rex7.calculator
import android.app.AlertDialog
import android.content.Context
import java.util.concurrent.Executors

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val bgExecutor = Executors.newSingleThreadExecutor()
    companion object {
        private const val PREFS_NAME = "calc_prefs"
        private const val KEY_HISTORY = "history_entries"
    }

    private lateinit var chassisLayout: LinearLayout
    private lateinit var lcdBorderLayout: LinearLayout
    private lateinit var lcdScreen: LinearLayout
    private lateinit var scrollExpression: HorizontalScrollView
    private lateinit var expressionContainer: View
    private lateinit var tvBrand: TextView
    private lateinit var tvLcdClock: TextView
    private lateinit var btnCopy: TextView
    private lateinit var tvExpression: TextView
    private lateinit var cursorBar: View
    private lateinit var tvResult: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnHistory: TextView

    private val allButtons = mutableListOf<AppCompatButton>()
    private val currentInput = StringBuilder(256)
    private var isEvaluated = false
    private var lastFullExpression = ""
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
        expressionContainer = findViewById(R.id.scrollExpression)
        tvBrand = findViewById(R.id.tvBrand)
        tvLcdClock = findViewById(R.id.tvLcdClock)
        btnCopy = findViewById(R.id.btnCopy)
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
        setupClipboard()

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
                if (!isEvaluated) {
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
            R.id.btnParen, R.id.btn0, R.id.btnDot, R.id.btnPlus,
            R.id.btnEqual
        )
        allButtons.clear()
        for (id in buttonIds) {
            findViewById<AppCompatButton>(id)?.let {
                it.backgroundTintList = null
                allButtons.add(it)
            }
        }
    }

    private fun setupClipboard() {
        // 1. COPY BUTTON: Pura calculation copy karega (e.g. 5+5+6-6+93 = 103)
        btnCopy.setOnClickListener {
            val textToCopy = if (isEvaluated && lastFullExpression.isNotEmpty()) {
                lastFullExpression
            } else if (currentInput.isNotEmpty()) {
                currentInput.toString()
            } else {
                ""
            }

            if (textToCopy.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("REX7 Calculation", textToCopy))
                Toast.makeText(this, "Copied: $textToCopy", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. PASTE: Expression, Cursor ya Screen par hold (long press) karke paste
        val pasteListener = View.OnLongClickListener {
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
                    Toast.makeText(this, "Pasted: $sanitized", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        tvExpression.setOnLongClickListener(pasteListener)
        cursorBar.setOnLongClickListener(pasteListener)
        expressionContainer.setOnLongClickListener(pasteListener)
        lcdScreen.setOnLongClickListener(pasteListener)
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
        val themeNames = arrayOf(
            "Classic Nokia (Original)",
            "Dark Retro",
            "Purple Pixel",
            "Orange & Cream",
            "Black & Yellow",
            "Royal Blue",
            "Graphite & Ice Blue"
        )
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

    private fun isOperatorBtn(id: Int): Boolean {
        return id == R.id.btnAC || id == R.id.btnDel || id == R.id.btnToggleSign ||
               id == R.id.btnPercent || id == R.id.btnDivide || id == R.id.btnMultiply ||
               id == R.id.btnMinus || id == R.id.btnPlus || id == R.id.btnEqual
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
                btnCopy.setTextColor(lcdText)

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
                btnCopy.setTextColor(darkLcdText)

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
                btnCopy.setTextColor(purpleLcdText)

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
            3 -> { // Orange & Cream
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F97316"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#EA580C"))
                }
                tvBrand.setTextColor(Color.WHITE)
                btnSettings.setTextColor(Color.WHITE)

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#7C2D12"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val mainText = Color.parseColor("#292524")
                tvExpression.setTextColor(mainText)
                cursorBar.setBackgroundColor(mainText)
                tvResult.setTextColor(mainText)
                tvLcdClock.setTextColor(mainText)
                btnCopy.setTextColor(mainText)

                val opColor = Color.parseColor("#9A3412")
                val opPress = Color.parseColor("#7C2D12")
                val opBorder = Color.parseColor("#C2410C")

                val numColor = Color.parseColor("#FFF7ED")
                val numPress = Color.parseColor("#FFEDD5")
                val numBorder = Color.parseColor("#FED7AA")

                for (btn in allButtons) {
                    if (isOperatorBtn(btn.id)) {
                        btn.background = makeBevel(opColor, opBorder, opPress, 12f)
                        btn.setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        btn.background = makeBevel(numColor, numBorder, numPress, 12f)
                        btn.setTextColor(mainText)
                    }
                }
            }
            4 -> { // Black & Yellow
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F4C542"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#DDA922"))
                }
                tvBrand.setTextColor(Color.parseColor("#292929"))
                btnSettings.setTextColor(Color.parseColor("#292929"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1F1F1F"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val mainText = Color.parseColor("#292929")
                tvExpression.setTextColor(mainText)
                cursorBar.setBackgroundColor(mainText)
                tvResult.setTextColor(mainText)
                tvLcdClock.setTextColor(mainText)
                btnCopy.setTextColor(mainText)

                val opColor = Color.parseColor("#292929")
                val opPress = Color.parseColor("#151515")
                val opBorder = Color.parseColor("#444444")

                val numColor = Color.parseColor("#FFF9E9")
                val numPress = Color.parseColor("#F5E6BD")
                val numBorder = Color.parseColor("#EAD69E")

                val yellowText = Color.parseColor("#F4C542")

                for (btn in allButtons) {
                    if (isOperatorBtn(btn.id)) {
                        btn.background = makeBevel(opColor, opBorder, opPress, 12f)
                        btn.setTextColor(yellowText)
                    } else {
                        btn.background = makeBevel(numColor, numBorder, numPress, 12f)
                        btn.setTextColor(mainText)
                    }
                }
            }
            5 -> { // Royal Blue
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2563EB"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#1D4ED8"))
                }
                tvBrand.setTextColor(Color.WHITE)
                btnSettings.setTextColor(Color.WHITE)

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#172554"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val mainText = Color.parseColor("#172554")
                tvExpression.setTextColor(mainText)
                cursorBar.setBackgroundColor(mainText)
                tvResult.setTextColor(mainText)
                tvLcdClock.setTextColor(mainText)
                btnCopy.setTextColor(mainText)

                val opColor = Color.parseColor("#1D4ED8")
                val opPress = Color.parseColor("#1E40AF")
                val opBorder = Color.parseColor("#3B82F6")

                val numColor = Color.parseColor("#EFF6FF")
                val numPress = Color.parseColor("#DBEAFE")
                val numBorder = Color.parseColor("#BFDBFE")

                for (btn in allButtons) {
                    if (isOperatorBtn(btn.id)) {
                        btn.background = makeBevel(opColor, opBorder, opPress, 12f)
                        btn.setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        btn.background = makeBevel(numColor, numBorder, numPress, 12f)
                        btn.setTextColor(mainText)
                    }
                }
            }
            6 -> { // Graphite & Ice Blue
                chassisLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#202124"))
                    cornerRadius = chassisRadius
                    setStroke((3 * density).toInt(), Color.parseColor("#303134"))
                }
                tvBrand.setTextColor(Color.parseColor("#65D6FF"))
                btnSettings.setTextColor(Color.parseColor("#65D6FF"))

                lcdBorderLayout.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#111214"))
                    cornerRadius = lcdRadius
                }
                lcdScreen.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = (lcdRadius - 2 * density)
                }

                val mainText = Color.parseColor("#202124")
                tvExpression.setTextColor(mainText)
                cursorBar.setBackgroundColor(mainText)
                tvResult.setTextColor(mainText)
                tvLcdClock.setTextColor(mainText)
                btnCopy.setTextColor(mainText)

                val opColor = Color.parseColor("#30343B")
                val opPress = Color.parseColor("#1E2024")
                val opBorder = Color.parseColor("#434852")

                val numColor = Color.parseColor("#F1F3F4")
                val numPress = Color.parseColor("#E0E3E6")
                val numBorder = Color.parseColor("#D1D5DB")

                val iceBlueText = Color.parseColor("#65D6FF")

                for (btn in allButtons) {
                    if (isOperatorBtn(btn.id)) {
                        btn.background = makeBevel(opColor, opBorder, opPress, 12f)
                        btn.setTextColor(iceBlueText)
                    } else {
                        btn.background = makeBevel(numColor, numBorder, numPress, 12f)
                        btn.setTextColor(mainText)
                    }
                }
            }
        }
        updateLcdLive()
    }

    private fun formatCommas(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (raw[i].isDigit()) {
                val start = i
                while (i < raw.length && raw[i].isDigit()) i++
                val num = raw.substring(start, i)
                if (num.length <= 3) {
                    sb.append(num)
                } else {
                    val last3 = num.substring(num.length - 3)
                    var rem = num.substring(0, num.length - 3)
                    val grps = mutableListOf<String>()
                    while (rem.length > 2) {
                        grps.add(0, rem.substring(rem.length - 2))
                        rem = rem.substring(0, rem.length - 2)
                    }
                    if (rem.isNotEmpty()) grps.add(0, rem)
                    sb.append(grps.joinToString(",") + "," + last3)
                }
            } else {
                sb.append(raw[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun handleSmartParen() {
        if (isEvaluated) {
            currentInput.setLength(0)
            isEvaluated = false
        }
        val text = currentInput.toString()
        var openCount = 0
        var closeCount = 0
        for (c in text) {
            if (c == '(') openCount++
            if (c == ')') closeCount++
        }
        if (text.isEmpty()) {
            currentInput.append("(")
        } else {
            val last = text.last()
            if (last.isDigit() || last == ')') {
                if (openCount > closeCount) {
                    currentInput.append(")")
                } else {
                    currentInput.append("×(")
                }
            } else {
                currentInput.append("(")
            }
        }
        updateLcdLive()
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

        findViewById<AppCompatButton>(R.id.btnParen)?.setOnClickListener { v ->
            triggerSound(v)
            if (isEvaluated) { currentInput.setLength(0); isEvaluated = false }
            handleSmartParen()
            updateLcdLive()


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
            lastFullExpression = ""
            tvExpression.text = ""
            cursorBar.visibility = View.GONE
            tvResult.text = ""
            tvExpression.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
        }
    }

        findViewById<AppCompatButton>(R.id.btnDel)?.setOnClickListener { v ->
            triggerSound(v)
            if (isEvaluated) {
                currentInput.setLength(0)
                isEvaluated = false
                lastFullExpression = ""
                tvExpression.text = ""
                cursorBar.visibility = View.GONE
                tvResult.text = ""
                tvExpression.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            }
    } else if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                updateLcdLive()
            }
        }

        findViewById<AppCompatButton>(R.id.btnEqual)?.setOnClickListener { v ->
            triggerSound(v)
            if (currentInput.isEmpty()) return@setOnClickListener
            val expr = currentInput.toString()
            val ans = CalculationEngine.evaluate(expr, isLivePreview = false)

            if (ans != null) {
                lastFullExpression = "$expr = $ans"
                currentInput.setLength(0)
                currentInput.append(ans)
                isEvaluated = true

                tvExpression.text = ans
                cursorBar.visibility = View.GONE
                tvResult.text = ""
                applySmartFontSize(ans)

                scrollExpression.post { scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }
                saveHistory(expr, ans)
            } else {
                tvExpression.text = "Error"
                cursorBar.visibility = View.GONE
                tvResult.text = ""
            }
        }

    private fun appendOperator(op: Char) {
        if (isEvaluated) isEvaluated = false
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
            tvExpression.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            return
        }

        tvExpression.text = s
        cursorBar.visibility = View.VISIBLE
        applySmartFontSize(s)

        scrollExpression.post { scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }

        val preview = CalculationEngine.evaluate(s, isLivePreview = true)
        if (preview != null && preview != s) {
            tvResult.text = preview
            val resLen = preview.length
            val resSp = when {
                resLen <= 10 -> 26f
                resLen <= 15 -> 22f
                resLen <= 20 -> 18f
                else -> 15f
            }
            tvResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, resSp)
            tvResult.alpha = 0.65f
        } else {
            tvResult.text = ""
        }
    }

    private fun applySmartFontSize(text: String) {
        val availableWidth = scrollExpression.width - scrollExpression.paddingLeft - scrollExpression.paddingRight
        val paint = tvExpression.paint

        var targetSp = 48f
        if (availableWidth > 0) {
            while (targetSp > 26f) {
                paint.textSize = targetSp * resources.displayMetrics.scaledDensity
                if (paint.measureText(text) + 20f <= availableWidth) break
                targetSp -= 2f
            }
        } else {
            val len = text.length
            targetSp = when {
                len <= 8 -> 48f
                len <= 12 -> 42f
                len <= 16 -> 36f
                len <= 22 -> 30f
                else -> 26f
            }
        }
        tvExpression.setTextSize(TypedValue.COMPLEX_UNIT_SP, targetSp)
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
                        applySmartFontSize(part)
                    }
                }
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear All") { _, _ ->
                bgExecutor.execute(object : Runnable { override fun run() { prefs.edit().remove(KEY_HISTORY).apply() } })
            }
            .show()
    }
}
