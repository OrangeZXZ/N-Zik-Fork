package app.n_zik.android.components.player.lyrics.utils

object HtmlDecoder {
    fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '&') {
                val end = text.indexOf(';', i + 1)
                if (end != -1 && end - i < 12) {
                    val entity = text.substring(i, end + 1)
                    val decoded = when {
                        entity == "&apos;" -> "'"
                        entity == "&quot;" -> "\""
                        entity == "&lt;" -> "<"
                        entity == "&gt;" -> ">"
                        entity == "&nbsp;" -> " "
                        entity == "&amp;" -> "&"
                        entity.startsWith("&#x") -> {
                            entity.substring(3, entity.length - 1).toIntOrNull(16)?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        entity.startsWith("&#") -> {
                            entity.substring(2, entity.length - 1).toIntOrNull()?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        else -> null
                    }
                    if (decoded != null) {
                        sb.append(decoded)
                        i = end + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
