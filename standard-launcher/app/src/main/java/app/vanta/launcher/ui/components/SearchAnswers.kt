package app.vanta.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract
import android.provider.Settings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

// Answers for the pull-down search: calculator, unit conversion, contacts, settings shortcuts.
// Pure functions except queryContacts (content resolver) and the settings runners (intents).

// ---------------------------------------------------------------- calculator

/**
 * Recursive-descent evaluator: numbers, + - * / ^ %, parentheses, unary minus. `x`/`×`/`÷` accepted.
 * Returns null unless the whole string parses and contains at least one binary operator
 * (so a bare "42" is not an answer).
 */
internal fun evalExpression(input: String): Double? {
    val p = Parser(input)
    val v = runCatching { p.expr() }.getOrNull() ?: return null
    p.skipWs()
    if (p.i != input.length || p.ops == 0 || !v.isFinite()) return null
    return v
}

private class Parser(private val s: String) {
    var i = 0
    var ops = 0
    private fun peek(): Char = if (i < s.length) s[i] else '\u0000'
    fun skipWs() { while (i < s.length && s[i] == ' ') i++ }

    fun expr(): Double {
        var v = term()
        while (true) {
            skipWs()
            val c = peek()
            if (c != '+' && c != '-') return v
            i++; ops++
            val r = term()
            v = if (c == '+') v + r else v - r
        }
    }

    private fun term(): Double {
        var v = power()
        while (true) {
            skipWs()
            val c = peek()
            when (c) {
                '*', 'x', 'X', '×' -> { i++; ops++; v *= power() }
                '/', '÷' -> { i++; ops++; v /= power() }
                '%' -> { i++; ops++; v %= power() }
                else -> return v
            }
        }
    }

    private fun power(): Double {
        val base = unary()
        skipWs()
        if (peek() != '^') return base
        i++; ops++
        return base.pow(power()) // right-associative
    }

    private fun unary(): Double {
        skipWs()
        return when (peek()) {
            '-' -> { i++; -unary() }
            '+' -> { i++; unary() }
            else -> atom()
        }
    }

    private fun atom(): Double {
        skipWs()
        if (peek() == '(') {
            i++
            val v = expr()
            skipWs()
            require(peek() == ')')
            i++
            return v
        }
        val start = i
        while (peek().isDigit() || peek() == '.') i++
        require(i > start)
        return s.substring(start, i).toDouble()
    }
}

/** "12*4" -> "12 × 4 = 48"; null when the query is not an arithmetic expression. */
internal fun calcAnswer(query: String): String? {
    val v = evalExpression(query.trim()) ?: return null
    val sb = StringBuilder()
    var prevWasValue = false
    for (ch in query.trim()) {
        when (ch) {
            ' ' -> {}
            '+', '-', '*', '/', '^', '%', 'x', 'X', '×', '÷' -> {
                val op = when (ch) { '*', 'x', 'X' -> '×'; '/' -> '÷'; else -> ch }
                if (prevWasValue) sb.append(' ').append(op).append(' ') else sb.append(op)
                prevWasValue = false
            }
            ')' -> { sb.append(ch); prevWasValue = true }
            '(' -> { sb.append(ch); prevWasValue = false }
            else -> { sb.append(ch); prevWasValue = true }
        }
    }
    return "$sb = ${fmt(v, 6)}"
}

private fun fmt(v: Double, decimals: Int): String {
    if (v == Math.rint(v) && abs(v) < 1e15) return String.format(Locale.US, "%.0f", v)
    val s = String.format(Locale.US, "%.${decimals}f", v).trimEnd('0').trimEnd('.')
    return if (s == "0" || s == "-0") String.format(Locale.US, "%.4g", v) else s
}

// ---------------------------------------------------------------- unit conversion

private val ConvertPattern = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*([a-z°]+)\\s+(?:to|in|as|->|=)\\s+([a-z°]+)$")

/** unit -> (dimension, factor to the base unit). Temperatures are handled separately. */
private val Units: Map<String, Pair<Char, Double>> = buildMap {
    fun put(dim: Char, factor: Double, vararg names: String) = names.forEach { put(it, dim to factor) }
    put('L', 1000.0, "km", "kilometer", "kilometers", "kilometre", "kilometres")
    put('L', 1.0, "m", "meter", "meters", "metre", "metres")
    put('L', 0.01, "cm", "centimeter", "centimeters")
    put('L', 0.001, "mm", "millimeter", "millimeters")
    put('L', 1609.344, "mi", "mile", "miles")
    put('L', 0.9144, "yd", "yard", "yards")
    put('L', 0.3048, "ft", "foot", "feet")
    put('L', 0.0254, "in", "inch", "inches")
    put('M', 1000.0, "kg", "kilo", "kilos", "kilogram", "kilograms")
    put('M', 1.0, "g", "gram", "grams")
    put('M', 453.59237, "lb", "lbs", "pound", "pounds")
    put('M', 28.349523125, "oz", "ounce", "ounces")
    put('V', 1.0, "l", "liter", "liters", "litre", "litres")
    put('V', 0.001, "ml", "milliliter", "milliliters")
    put('V', 3.785411784, "gal", "gallon", "gallons")
    put('T', 0.0, "c", "°c", "celsius")
    put('T', 1.0, "f", "°f", "fahrenheit")
}

/** "5 km to mi" -> "5 km = 3.11 mi"; also "72 f to c", "10 kg in lb", "3 in to cm". */
internal fun convertAnswer(query: String): String? {
    val m = ConvertPattern.find(query.trim().lowercase(Locale.US)) ?: return null
    val (num, fromName, toName) = m.destructured
    val from = Units[fromName] ?: return null
    val to = Units[toName] ?: return null
    if (from.first != to.first) return null
    val v = num.toDoubleOrNull() ?: return null
    val out = if (from.first == 'T') {
        when {
            from.second == to.second -> v
            from.second == 0.0 -> v * 9 / 5 + 32
            else -> (v - 32) * 5 / 9
        }
    } else v * from.second / to.second
    val shortFrom = Units.entries.first { it.value == from }.key
    val shortTo = Units.entries.first { it.value == to }.key
    return "$num $shortFrom = ${fmt(out, 2)} $shortTo"
}

// ---------------------------------------------------------------- contacts

internal class ContactHit(val id: Long, val name: String, val number: String?)

/** Contacts whose display name contains [query], primary number first, deduped by contact. Needs READ_CONTACTS. */
internal fun queryContacts(context: Context, query: String, limit: Int = 5): List<ContactHit> {
    val out = ArrayList<ContactHit>(limit)
    val seen = HashSet<Long>()
    val uri = Phone.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, (limit * 8).toString())
        .build()
    val projection = arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER)
    val sort = "${Phone.DISPLAY_NAME} ASC, ${Phone.IS_SUPER_PRIMARY} DESC, ${Phone.IS_PRIMARY} DESC"
    runCatching {
        context.contentResolver.query(uri, projection, "${Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$query%"), sort)?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(0)
                if (!seen.add(id)) continue
                val name = c.getString(1) ?: continue
                out.add(ContactHit(id, name, c.getString(2)))
            }
        }
    }
    return out
}

// ---------------------------------------------------------------- settings shortcuts

/** A settings row: [label] and what tapping it does, given the context and the dark-mode setter. */
internal class SettingsHit(val label: String, val run: (Context, (Boolean) -> Unit) -> Unit)

private fun opens(vararg intents: Intent): (Context, (Boolean) -> Unit) -> Unit = { ctx, _ ->
    intents.any { i -> runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess }
}

internal fun settingsHit(query: String): SettingsHit? {
    val q = query.trim().lowercase(Locale.US)
    if (q.length < 3) return null
    return when {
        "flashlight" in q || "torch" in q -> SettingsHit("FLASHLIGHT") { ctx, _ -> toggleTorch(ctx) }
        "wifi" in q || "wi-fi" in q -> SettingsHit("WI-FI", opens(Intent(Settings.Panel.ACTION_WIFI)))
        "internet" in q || "mobile data" in q -> SettingsHit("INTERNET", opens(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)))
        "bluetooth" in q -> SettingsHit("BLUETOOTH", opens(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
        "nfc" in q -> SettingsHit("NFC", opens(Intent(Settings.Panel.ACTION_NFC)))
        "dnd" in q || "do not disturb" in q -> SettingsHit(
            "DO NOT DISTURB",
            opens(Intent("android.settings.ZEN_MODE_SETTINGS"), Intent(Settings.ACTION_SOUND_SETTINGS))
        )
        "airplane" in q || "flight mode" in q -> SettingsHit("AIRPLANE MODE", opens(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)))
        "brightness" in q -> SettingsHit("BRIGHTNESS", opens(Intent(Settings.ACTION_DISPLAY_SETTINGS)))
        "hotspot" in q || "tether" in q -> SettingsHit(
            "HOTSPOT",
            opens(
                Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            )
        )
        "dark" in q -> SettingsHit("DARK MODE") { _, setDark -> setDark(true) }
        "light" in q -> SettingsHit("LIGHT MODE") { _, setDark -> setDark(false) }
        else -> null
    }
}

// ponytail: not synced with the system torch tile; register a TorchCallback if drift annoys anyone.
private var torchOn = false

private fun toggleTorch(context: Context) {
    runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        torchOn = !torchOn
        cm.setTorchMode(id, torchOn)
    }
}

// ---------------------------------------------------------------- self-check

/** Smallest thing that fails if the parser or converter breaks. Call from a debug hook; throws on failure. */
internal fun selfTest(): Boolean {
    check(evalExpression("12*4") == 48.0) { "mul" }
    check(evalExpression("2^3^2") == 512.0) { "pow right-assoc" }
    check(evalExpression("-(3+4)*2") == -14.0) { "unary + parens" }
    check(evalExpression("7 % 3") == 1.0 && evalExpression("5") == null) { "mod / bare number" }
    check(calcAnswer("12*4") == "12 × 4 = 48") { "calc format" }
    check(convertAnswer("5 km to mi") == "5 km = 3.11 mi") { "km->mi" }
    check(convertAnswer("72 f to c") == "72 f = 22.22 c") { "f->c" }
    check(convertAnswer("3 in to cm") == "3 in = 7.62 cm") { "in->cm" }
    return true
}
