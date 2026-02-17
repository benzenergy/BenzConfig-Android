package com.example.benzconfig

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.TextWatcher
import android.text.Editable
import android.text.Html
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // Пропорции (сбрасываются при перезапуске)
    private var summerCityProp = 0.3
    private var summerHighwayProp = 0.7
    private var winterCityProp = 0.3
    private var winterHighwayProp = 0.7

    // Нормы расхода (сохраняются)
    private var summerCityRate = 11.5
    private var summerHighwayRate = 8.5
    private var winterCityRate = 13.8
    private var winterHighwayRate = 10.2

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val rootView = findViewById<ScrollView>(R.id.scrollView) // или ваш корневой layout
        rootView.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
            } else {

                insets.systemWindowInsetTop
            }

            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, view.paddingBottom)
            insets
        }
        rootView.requestApplyInsets()

        rootView.setOnTouchListener { _, _ ->
            clearAllErrors()
            rootView.performClick()
            false
        }

        prefs = getSharedPreferences("benz_prefs", Context.MODE_PRIVATE)
        loadRates()

        val inputSummer = findViewById<EditText>(R.id.inputSummer)
        val inputWinter = findViewById<EditText>(R.id.inputWinter)

        clearErrorOnFocusLost(inputSummer)
        clearErrorOnFocusLost(inputWinter)

        val outputSummer = findViewById<TextView>(R.id.outputSummer)
        val outputWinter = findViewById<TextView>(R.id.outputWinter)

        setupInputFilters(inputSummer, 10)
        setupInputFilters(inputWinter, 10)

        // Кнопки рассчитать
        findViewById<FrameLayout>(R.id.btnSummerCalcWrapper).setOnClickListener {
            calculate(inputSummer, outputSummer, true)
        }

        findViewById<FrameLayout>(R.id.btnWinterCalcWrapper).setOnClickListener {
            calculate(inputWinter, outputWinter, false)
        }

        // Кнопка Настройки
        val settingsBtn = findViewById<FrameLayout>(R.id.btnSummerSettingsWrapper)
        settingsBtn.setOnClickListener {
            showSettingsDialog(true)
        }

        val winterSettingsBtn = findViewById<FrameLayout>(R.id.btnWinterSettingsWrapper)
        winterSettingsBtn.setOnClickListener {
            showSettingsDialog(false)
        }

        // Кнопка About
        findViewById<Button>(R.id.btnAbout).setOnClickListener { showAboutDialog() }
    }

    private fun clearAllErrors() {
        val inputs = listOf(
            findViewById<EditText>(R.id.inputSummer),
            findViewById<EditText>(R.id.inputWinter)
        )
        inputs.forEach { it.setBackgroundResource(R.drawable.bg_input) }
    }

    private fun clearErrorOnFocusLost(editText: EditText) {
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editText.setBackgroundResource(R.drawable.bg_input)
            }
        }
    }

    // Функция расчёта
    private fun calculate(input: EditText, output: TextView, isSummer: Boolean) {
        val text = input.text.toString().replace(',', '.')

        if (text.isEmpty()) {
            input.setBackgroundResource(R.drawable.bg_error)
            return
        }

        val distance = text.toDoubleOrNull()
        if (distance == null || distance < 0) {
            input.setBackgroundResource(R.drawable.bg_error)
            return
        }

        input.setBackgroundResource(R.drawable.bg_input)

        val propCity = if (isSummer) summerCityProp else winterCityProp
        val propHighway = if (isSummer) summerHighwayProp else winterHighwayProp
        val rateCity = if (isSummer) summerCityRate else winterCityRate
        val rateHighway = if (isSummer) summerHighwayRate else winterHighwayRate

        val roadCity = (distance * propCity)
        val roadHighway = (distance * propHighway)
        val fuelCity = roadCity / 100 * rateCity
        val fuelHighway = roadHighway / 100 * rateHighway
        val total = fuelCity + fuelHighway

        val cityKm = if (roadCity % 1.0 == 0.0) roadCity.toInt().toString() else "%.2f".format(roadCity)
        val highwayKm = if (roadHighway % 1.0 == 0.0) roadHighway.toInt().toString() else "%.2f".format(roadHighway)

        val resultText = """
                        Общий расход: ${"%.2f".format(total)} л
                    
                        Детализация
                        Пробег по городу: $cityKm км
                        Пробег по трассе: $highwayKm км
                    
                        Нормы расхода
                        Город: ${"%.1f".format(rateCity)} л на 100 км
                        Трасса: ${"%.1f".format(rateHighway)} л на 100 км
                    
                        Пропорции
                        Городской режим: ${(propCity*100).toInt()}%
                        Трассовый режим: ${(propHighway*100).toInt()}%
                    """.trimIndent()


        typeWriter(output, resultText)
    }

    // Эффект печатной машинки
    private fun typeWriter(view: TextView, text: String, delayMs: Long = 8) {
        view.text = ""
        CoroutineScope(Dispatchers.Main).launch {
            for (char in text) {
                view.append(char.toString())
                delay(delayMs)
            }
        }
    }

    // Фильтр ввода и автозамена запятой
    private fun setupInputFilters(editText: EditText, maxLength: Int = 4) {
        editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().replace(',', '.')
                if (str != s.toString()) {
                    editText.setText(str)
                    editText.setSelection(str.length)
                }
            }
        })
    }

    // Диалоги настроек (лето/зима)
    private fun showSettingsDialog(isSummer: Boolean) {
        val layoutId = if (isSummer) R.layout.dialog_summer_settings else R.layout.dialog_winter_settings
        val dialogView = layoutInflater.inflate(layoutId, null)

        val cityProp = dialogView.findViewById<EditText>(R.id.cityProp)
        val highwayProp = dialogView.findViewById<EditText>(R.id.highwayProp)
        val cityRate = dialogView.findViewById<EditText>(R.id.cityRate)
        val highwayRate = dialogView.findViewById<EditText>(R.id.highwayRate)

        // Заполняем значения
        if (isSummer) {
            cityProp.setText((summerCityProp * 100).toInt().toString())
            highwayProp.setText((summerHighwayProp * 100).toInt().toString())
            cityRate.setText("%.1f".format(summerCityRate))
            highwayRate.setText("%.1f".format(summerHighwayRate))
        } else {
            cityProp.setText((winterCityProp * 100).toInt().toString())
            highwayProp.setText((winterHighwayProp * 100).toInt().toString())
            cityRate.setText("%.1f".format(winterCityRate))
            highwayRate.setText("%.1f".format(winterHighwayRate))
        }

        // Фильтры
        listOf(cityProp, highwayProp, cityRate, highwayRate).forEach {
            setupInputFilters(it, 4)

            // Убираем красную рамку при фокусе
            it.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) v.setBackgroundResource(R.drawable.bg_input)
            }
        }

        fun validateAndSync(changed: EditText, other: EditText) {
            val value = changed.text.toString().replace(',', '.').toDoubleOrNull()
            if (value != null && value in 0.0..100.0) {
                val second = 100 - value
                other.setText(second.toInt().toString())
                changed.setBackgroundResource(R.drawable.bg_input)
                other.setBackgroundResource(R.drawable.bg_input)
            } else {
                changed.setBackgroundResource(R.drawable.bg_error)
            }
        }

        cityProp.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndSync(cityProp, highwayProp)
        }

        highwayProp.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndSync(highwayProp, cityProp)
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Закрыть", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            saveBtn.setOnClickListener {
                val cp = cityProp.text.toString().toDoubleOrNull()
                val hp = highwayProp.text.toString().toDoubleOrNull()
                val cr = cityRate.text.toString().replace(',', '.').toDoubleOrNull()
                val hr = highwayRate.text.toString().replace(',', '.').toDoubleOrNull()

                var valid = true

                if (cp == null || hp == null || kotlin.math.abs(cp + hp - 100.0) > 0.01) {
                    cityProp.setBackgroundResource(R.drawable.bg_error)
                    highwayProp.setBackgroundResource(R.drawable.bg_error)
                    valid = false
                }

                if (cr == null) {
                    cityRate.setBackgroundResource(R.drawable.bg_error)
                    valid = false
                }

                if (hr == null) {
                    highwayRate.setBackgroundResource(R.drawable.bg_error)
                    valid = false
                }

                if (!valid) return@setOnClickListener

                if (isSummer) {
                    summerCityProp = cp!! / 100
                    summerHighwayProp = hp!! / 100
                    summerCityRate = cr!!
                    summerHighwayRate = hr!!
                } else {
                    winterCityProp = cp!! / 100
                    winterHighwayProp = hp!! / 100
                    winterCityRate = cr!!
                    winterHighwayRate = hr!!
                }

                saveRates()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // About
    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)

        val licenseText = dialogView.findViewById<TextView>(R.id.licenseText)
        licenseText.text = Html.fromHtml(
            "Лицензия <a href='https://www.gnu.org/licenses/gpl-3.0.html'>GNU GPL v3.0</a>",
            Html.FROM_HTML_MODE_LEGACY
        )
        licenseText.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        val flaticonText = dialogView.findViewById<TextView>(R.id.flaticonText)
        flaticonText.text = Html.fromHtml(
            "Материалы <a href='https://www.flaticon.com/free-icon/sign_2737912'>flaticon.com</a>",
            Html.FROM_HTML_MODE_LEGACY
        )
        flaticonText.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        val githubText = dialogView.findViewById<TextView>(R.id.githubText)
        githubText.text = Html.fromHtml(
            "Исходник <a href='https://github.com/benzenergy/BenzConfig-Android'>github.com</a><br/>",
            Html.FROM_HTML_MODE_LEGACY
        )
        githubText.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()

        dialog.show()

        val closeBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        closeBtn.setTextColor(Color.parseColor("#FFFFFF"))

    }


    // Сохранение норм
    private fun saveRates() {
        prefs.edit()
            .putString("summerCityRate", summerCityRate.toString())
            .putString("summerHighwayRate", summerHighwayRate.toString())
            .putString("winterCityRate", winterCityRate.toString())
            .putString("winterHighwayRate", winterHighwayRate.toString())
            .apply()
    }

    // Загрузка норм
    private fun loadRates() {
        summerCityRate = prefs.getString("summerCityRate", "11.5")?.toDoubleOrNull() ?: 11.5
        summerHighwayRate = prefs.getString("summerHighwayRate", "8.5")?.toDoubleOrNull() ?: 8.5
        winterCityRate = prefs.getString("winterCityRate", "13.8")?.toDoubleOrNull() ?: 13.8
        winterHighwayRate = prefs.getString("winterHighwayRate", "10.2")?.toDoubleOrNull() ?: 10.2
    }
}
