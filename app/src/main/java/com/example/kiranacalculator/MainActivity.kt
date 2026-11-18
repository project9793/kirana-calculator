package com.example.kiranacalculator

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Kirana Calculator.
 *
 * Three modes share one screen:
 *  - MONEY  (पैसा निकालें): वजन + रेट  -> रकम
 *  - GRAMS  (ग्राम निकालें): रकम + रेट  -> वजन (ग्राम)
 *  - BILL   (बिल जोड़ें):  रकम जोड़ते जाएँ -> कुल बिल
 *
 * All arithmetic happens on the device, nothing goes to the network.
 */
class MainActivity : Activity() {

    private enum class Mode { MONEY, GRAMS, BILL }

    private lateinit var tabMoney: TextView
    private lateinit var tabGrams: TextView
    private lateinit var tabBill: TextView

    private lateinit var displayLabel: TextView
    private lateinit var displayValue: TextView
    private lateinit var displaySub: TextView

    private lateinit var inputCard: LinearLayout
    private lateinit var inputLabel: TextView
    private lateinit var inputSuffix: TextView
    private lateinit var inputValue: TextView

    private lateinit var rateCard: LinearLayout
    private lateinit var rateValue: TextView

    private var mode = Mode.GRAMS

    /** Text typed into the first card (amount, or weight in money mode). */
    private var inputText = ""

    /** Text typed into the rate card (₹ per kilo). */
    private var rateText = ""

    /** Last computed result, kept on screen while the next entry is typed. */
    private var lastResult = ""

    private var billTotal = 0.0
    private var billCount = 0

    /** True while the rate card is the active input. */
    private var editingRate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabMoney = findViewById(R.id.tabMoney)
        tabGrams = findViewById(R.id.tabGrams)
        tabBill = findViewById(R.id.tabBill)

        displayLabel = findViewById(R.id.displayLabel)
        displayValue = findViewById(R.id.displayValue)
        displaySub = findViewById(R.id.displaySub)

        inputCard = findViewById(R.id.inputCard)
        inputLabel = findViewById(R.id.inputLabel)
        inputSuffix = findViewById(R.id.inputSuffix)
        inputValue = findViewById(R.id.inputValue)

        rateCard = findViewById(R.id.rateCard)
        rateValue = findViewById(R.id.rateValue)

        tabMoney.setOnClickListener { applyMode(Mode.MONEY) }
        tabGrams.setOnClickListener { applyMode(Mode.GRAMS) }
        tabBill.setOnClickListener { applyMode(Mode.BILL) }

        inputCard.setOnClickListener {
            editingRate = false
            refresh()
        }
        rateCard.setOnClickListener {
            editingRate = true
            refresh()
        }

        listOf(
            R.id.key0 to "0",
            R.id.key1 to "1",
            R.id.key2 to "2",
            R.id.key3 to "3",
            R.id.key4 to "4",
            R.id.key5 to "5",
            R.id.key6 to "6",
            R.id.key7 to "7",
            R.id.key8 to "8",
            R.id.key9 to "9",
            R.id.key00 to "00",
            R.id.keyDot to "."
        ).forEach { (id, label) ->
            findViewById<View>(id).setOnClickListener { onDigit(label) }
        }

        findViewById<View>(R.id.keyBack).setOnClickListener { onBackspace() }
        findViewById<View>(R.id.keyClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.keySubmit).setOnClickListener { onSubmit() }

        applyMode(Mode.GRAMS)
    }

    // ---------------------------------------------------------------- modes

    private fun applyMode(newMode: Mode) {
        mode = newMode
        inputText = ""
        rateText = ""
        lastResult = ""
        editingRate = false
        billTotal = 0.0
        billCount = 0

        when (newMode) {
            Mode.GRAMS -> {
                displayLabel.setText(R.string.label_weight)
                inputLabel.setText(R.string.label_amount)
                inputSuffix.setText(R.string.rupee)
                rateCard.visibility = View.VISIBLE
            }
            Mode.MONEY -> {
                displayLabel.setText(R.string.label_amount)
                inputLabel.setText(R.string.label_weight)
                inputSuffix.setText(R.string.unit_gram)
                rateCard.visibility = View.VISIBLE
            }
            Mode.BILL -> {
                displayLabel.setText(R.string.label_total_bill)
                inputLabel.setText(R.string.label_amount)
                inputSuffix.setText(R.string.rupee)
                rateCard.visibility = View.GONE
            }
        }

        highlightTabs()
        refresh()
    }

    private fun highlightTabs() {
        val tabs = listOf(
            Mode.MONEY to tabMoney,
            Mode.GRAMS to tabGrams,
            Mode.BILL to tabBill
        )
        for ((tabMode, view) in tabs) {
            if (tabMode == mode) {
                view.setBackgroundResource(R.drawable.bg_tab_selected)
                view.setTextColor(getColor(R.color.card))
            } else {
                view.setBackgroundResource(0)
                view.setTextColor(getColor(R.color.text_secondary))
            }
        }
    }

    // -------------------------------------------------------------- keypad

    private fun onDigit(digit: String) {
        val current = if (editingRate) rateText else inputText
        val next = when {
            digit == "." && current.contains(".") -> return
            digit == "." && current.isEmpty() -> "0."
            current == "0" && digit != "." && digit != "00" -> digit
            current.length >= 9 -> return
            else -> current + digit
        }
        if (editingRate) rateText = next else inputText = next
        refresh()
    }

    private fun onBackspace() {
        if (editingRate) {
            rateText = rateText.dropLast(1)
        } else {
            inputText = inputText.dropLast(1)
        }
        refresh()
    }

    private fun onClear() {
        val wasEmpty = if (editingRate) rateText.isEmpty() else inputText.isEmpty()
        if (editingRate) rateText = "" else inputText = ""

        if (mode == Mode.BILL && !editingRate && wasEmpty) {
            // A second press of C on an empty amount wipes the whole bill.
            billTotal = 0.0
            billCount = 0
            lastResult = ""
        } else if (inputText.isEmpty() && rateText.isEmpty()) {
            lastResult = ""
        }
        refresh()
    }

    private fun onSubmit() {
        when (mode) {
            Mode.BILL -> {
                val value = inputText.toDoubleOrNull()
                if (value != null && value > 0.0) {
                    billTotal += value
                    billCount += 1
                    inputText = ""
                }
            }
            else -> {
                // The result is already on screen; clear the entry so the next
                // one can be typed straight away (the rate is kept, as in a shop).
                if (compute() != null) {
                    inputText = ""
                }
            }
        }
        refresh()
    }

    // --------------------------------------------------------------- state

    private fun refresh() {
        inputValue.text = inputText
        rateValue.text = rateText

        val inputActive = mode == Mode.BILL || !editingRate
        inputCard.setBackgroundResource(
            if (inputActive) R.drawable.bg_card_active else R.drawable.bg_card
        )
        rateCard.setBackgroundResource(
            if (editingRate) R.drawable.bg_card_active else R.drawable.bg_card
        )

        val computed = compute()
        if (computed != null) {
            lastResult = computed
        }
        displayValue.text = if (lastResult.isEmpty()) emptyResult() else lastResult

        if (mode == Mode.BILL) {
            displaySub.visibility = View.VISIBLE
            displaySub.text = getString(R.string.items_count, billCount)
        } else {
            displaySub.visibility = View.GONE
        }
    }

    /** Returns the formatted result, or null when there is not enough input yet. */
    private fun compute(): String? {
        return when (mode) {
            Mode.GRAMS -> {
                val amount = inputText.toDoubleOrNull()
                val rate = rateText.toDoubleOrNull()
                if (amount == null || rate == null || rate <= 0.0) {
                    null
                } else {
                    fmt(amount / rate * 1000.0) + " " + getString(R.string.unit_gram)
                }
            }
            Mode.MONEY -> {
                val grams = inputText.toDoubleOrNull()
                val rate = rateText.toDoubleOrNull()
                if (grams == null || rate == null) {
                    null
                } else {
                    getString(R.string.rupee) + fmt(grams / 1000.0 * rate)
                }
            }
            Mode.BILL -> getString(R.string.rupee) + fmt(billTotal)
        }
    }

    private fun emptyResult(): String {
        return if (mode == Mode.GRAMS) {
            "0 " + getString(R.string.unit_gram)
        } else {
            getString(R.string.rupee) + "0"
        }
    }

    // ------------------------------------------------------------ numbers

    /** Rounds to 2 decimals, drops trailing zeros, groups digits Indian style. */
    private fun fmt(value: Double): String {
        if (value == 0.0 || value.isNaN() || value.isInfinite()) return "0"

        val rounded = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
        var plain = rounded.toPlainString()
        if (plain == "0") return "0"

        val negative = plain.startsWith("-")
        if (negative) plain = plain.substring(1)

        val dot = plain.indexOf('.')
        val integerPart = if (dot >= 0) plain.substring(0, dot) else plain
        val fraction = if (dot >= 0) plain.substring(dot) else ""

        return (if (negative) "-" else "") + groupIndian(integerPart) + fraction
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits

        val last3 = digits.substring(digits.length - 3)
        var rest = digits.substring(0, digits.length - 3)
        val builder = StringBuilder()

        while (rest.length > 2) {
            builder.insert(0, "," + rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        if (rest.isNotEmpty()) builder.insert(0, rest)

        return builder.toString() + "," + last3
    }
}
