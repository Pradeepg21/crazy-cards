package com.satya.uno.ui

import android.content.Context
import java.util.Locale

/**
 * A language the rules are available in. The text lives in `assets/rules/<code>.txt` and only the
 * chosen language is read, so adding languages doesn't grow memory use.
 *
 * File format: first line is the screen title, then each section starts with a `## Heading` line.
 * Card and button names (Skip, Wild, UNO, Rejoin…) stay in English, as they appear in the app.
 */
data class RulesLang(val code: String, val label: String, val rtl: Boolean = false) {
    class Text(val title: String, val sections: List<Pair<String, String>>)

    fun load(context: Context): Text {
        val lines = context.assets.open("rules/$code.txt").bufferedReader().use { it.readLines() }
        val title = lines.first().trim()
        val sections = mutableListOf<Pair<String, String>>()
        var heading: String? = null
        val body = StringBuilder()
        fun flush() {
            heading?.let { sections += it to body.toString().trim() }
            body.clear()
        }
        for (line in lines.drop(1)) {
            if (line.startsWith("## ")) {
                flush()
                heading = line.removePrefix("## ").trim()
            } else {
                body.append(line).append('\n')
            }
        }
        flush()
        return Text(title, sections)
    }

    companion object {
        val ALL = listOf(
            RulesLang("en", "English"),
            // India
            RulesLang("hi", "हिन्दी"),
            RulesLang("bn", "বাংলা"),
            RulesLang("ta", "தமிழ்"),
            RulesLang("te", "తెలుగు"),
            RulesLang("mr", "मराठी"),
            RulesLang("gu", "ગુજરાતી"),
            RulesLang("kn", "ಕನ್ನಡ"),
            RulesLang("ml", "മലയാളം"),
            RulesLang("pa", "ਪੰਜਾਬੀ"),
            RulesLang("or", "ଓଡ଼ିଆ"),
            RulesLang("ur", "اردو", rtl = true),
            RulesLang("ne", "नेपाली"),
            // Rest of the world
            RulesLang("es", "Español"),
            RulesLang("fr", "Français"),
            RulesLang("de", "Deutsch"),
            RulesLang("pt", "Português"),
            RulesLang("it", "Italiano"),
            RulesLang("ru", "Русский"),
            RulesLang("tr", "Türkçe"),
            RulesLang("pl", "Polski"),
            RulesLang("nl", "Nederlands"),
            RulesLang("zh", "简体中文"),
            RulesLang("ja", "日本語"),
            RulesLang("ko", "한국어"),
            RulesLang("id", "Bahasa Indonesia"),
            RulesLang("vi", "Tiếng Việt"),
            RulesLang("th", "ไทย"),
            RulesLang("fil", "Filipino"),
            RulesLang("ar", "العربية", rtl = true),
            RulesLang("fa", "فارسی", rtl = true),
        )

        /** The saved choice, else the phone's language if supported, else English. */
        fun of(code: String?): RulesLang {
            ALL.firstOrNull { it.code == code }?.let { return it }
            // toLanguageTag gives modern codes ("id", "fil") where Locale.language may give "in"/"tl".
            val phone = when (val tag = Locale.getDefault().toLanguageTag().substringBefore('-')) {
                "in" -> "id"
                "tl" -> "fil"
                else -> tag
            }
            return ALL.firstOrNull { it.code == phone } ?: ALL.first()
        }
    }
}
