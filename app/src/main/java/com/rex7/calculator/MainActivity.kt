package com.rex7.calculator

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvSideBadge: TextView
    private lateinit var prefs: SharedPreferences
    private var toneGenerator: ToneGenerator? = null
    private var isSoundOn = true
    private var currentTheme = 0

    private var currentInput = StringBuilder()
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("rex7_prefs", Context.MODE_PRIVATE)
        isSoundOn = prefs.getBoolean("sound_enabled", true)
        currentTheme = prefs.getInt("app_theme", 0)

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
        } catch (_: Exception) {}

        initViews()
        applyTheme(currentTheme)
        loadHistory()
    }

    private fun initViews() {
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        tvSideBadge = findViewById(R.id.tvSideBadge)

        val btnSoundToggle: Button = findViewById(R.id.btnSoundToggle)
        val btnThemeToggle: Button = findViewById(R.id.btnThemeToggle)
        val btnHistory: Button = findViewById(R.id.btnHistory)

        btnSoundToggle.text = if (isSoundOn) "SND: ON" else "SND: OFF"
        btnSoundToggle.setOnClickListener {
            playClick()
            isSoundOn = !isSoundOn
            btnSoundToggle.text = if (isSoundOn) "SND: ON" else "SND: OFF"
            prefs.edit().putBoolean("sound_enabled", isSoundOn).apply()
        }

        btnThemeToggle.setOnClickListener {
            playClick()
            currentTheme = (currentTheme + 1) % 3
            prefs.edit().putInt("app_theme", currentTheme).apply()
            applyTheme(currentTheme)
        }

        btnHistory.setOnClickListener {
            playClick()
            showHistoryDialog()
        }

        val buttons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9", R.id.btnAdd to "+", R.id.btnSub to "−",
            R.id.btnMul to "×", R.id.btnDiv to "÷", R.id.btnPercent to "%", R.id.btnDot to "."
        )

        for ((id, char) in buttons) {
            findViewById<Button>(id).setOnClickListener {
                playClick()
                appendCharacter(char)
            }
        }

        findViewById<Button>(R.id.btnAC).setOnClickListener {
            playClick()
            clearAll()
        }

        findViewById<Button>(R.id.btnDel).setOnClickListener {
            playClick()
            deleteLast()
        }

        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener {
            playClick()
            toggleSign()
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            playClick()
            calculateResult()
        }
    }

    private fun playClick() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (isSoundOn) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        }
    }

    private fun appendCharacter(str: String) {
        currentInput.append(str)
        tvExpression.text = currentInput.toString()
    }

    private fun clearAll() {
        currentInput.clear()
        tvExpression.text = ""
        tvResult.text = "0"
    }

    private fun deleteLast() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            tvExpression.text = currentInput.toString()
            if (currentInput.isEmpty()) tvResult.text = "0"
        }
    }

    private fun toggleSign() {
        if (currentInput.isNotEmpty()) {
            val expr = currentInput.toString()
            currentInput = if (expr.startsWith("-")) {
                StringBuilder(expr.substring(1))
            } else {
                StringBuilder("-$expr")
            }
            tvExpression.text = currentInput.toString()
        }
    }

    private fun calculateResult() {
        val expr = currentInput.toString()
        if (expr.isBlank()) return
        try {
            val res = ExpressionEvaluator.evaluate(expr)
            val resultFormatted = if (res % 1.0 == 0.0) {
                res.toLong().toString()
            } else {
                "%.4f".format(res).trimEnd('0').trimEnd('.')
            }
            tvResult.text = resultFormatted
            val entry = "$expr = $resultFormatted"
            saveToHistory(entry)
        } catch (e: ArithmeticException) {
            tvResult.text = "Error: Div/0"
        } catch (e: Exception) {
            tvResult.text = "Syntax Error"
        }
    }

    private fun saveToHistory(entry: String) {
        historyList.add(0, entry)
        if (historyList.size > 30) historyList.removeAt(historyList.size - 1)
        val historyStr = historyList.joinToString(";;")
        prefs.edit().putString("calc_history", historyStr).apply()
    }

    private fun loadHistory() {
        val historyStr = prefs.getString("calc_history", "") ?: ""
        if (historyStr.isNotEmpty()) {
            historyList.clear()
            historyList.addAll(historyStr.split(";;"))
        }
    }

    private fun showHistoryDialog() {
        if (historyList.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Calculation History")
                .setMessage("No history yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val items = historyList.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("History (Tap to Reuse)")
            .setItems(items) { _, which ->
                val selected = items[which]
                val value = selected.substringAfterLast("=").trim()
                currentInput = StringBuilder(value)
                tvExpression.text = currentInput.toString()
                tvResult.text = value
            }
            .setNeutralButton("Clear All") { _, _ ->
                historyList.clear()
                prefs.edit().remove("calc_history").apply()
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun applyTheme(themeIndex: Int) {
        val layout = findViewById<LinearLayout>(R.id.mainLayout)
        val lcd = findViewById<LinearLayout>(R.id.lcdScreen)
        val tvBrand = findViewById<TextView>(R.id.tvBrand)

        when (themeIndex) {
            1 -> { // Dark Retro
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_body))
                lcd.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_lcd))
                tvExpression.setTextColor(ContextCompat.getColor(this, R.color.dark_lcd_text))
                tvResult.setTextColor(ContextCompat.getColor(this, R.color.dark_lcd_text))
                tvBrand.setTextColor(ContextCompat.getColor(this, R.color.dark_btn_text))
                tvSideBadge.setTextColor(ContextCompat.getColor(this, R.color.dark_badge))
            }
            2 -> { // Purple Pixel
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_body))
                lcd.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_lcd))
                tvExpression.setTextColor(ContextCompat.getColor(this, R.color.purple_lcd_text))
                tvResult.setTextColor(ContextCompat.getColor(this, R.color.purple_lcd_text))
                tvBrand.setTextColor(ContextCompat.getColor(this, R.color.purple_btn_text))
                tvSideBadge.setTextColor(ContextCompat.getColor(this, R.color.purple_badge))
            }
            else -> { // Classic Nokia
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.nokia_body))
                lcd.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.nokia_lcd))
                tvExpression.setTextColor(ContextCompat.getColor(this, R.color.nokia_lcd_text))
                tvResult.setTextColor(ContextCompat.getColor(this, R.color.nokia_lcd_text))
                tvBrand.setTextColor(0xFF3D3833.toInt())
                tvSideBadge.setTextColor(ContextCompat.getColor(this, R.color.nokia_badge))
            }
        }
    }
}
