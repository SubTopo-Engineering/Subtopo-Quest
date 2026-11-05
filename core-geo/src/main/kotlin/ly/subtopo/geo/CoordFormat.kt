package ly.subtopo.geo

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** Formatage et lecture des coordonnées : degrés décimaux et degrés-minutes-secondes. */
object CoordFormat {
    fun decimal(value: Double, decimals: Int = 6) = String.format(Locale.ROOT, "%.${decimals}f", value)

    /** Ex : 32°53'14.00"N. [isLat] choisit N/S ou E/W. */
    fun dms(value: Double, isLat: Boolean, secDecimals: Int = 2): String {
        val hemi = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "W")
        var v = abs(value)
        var d = floor(v).toInt()
        var m = floor((v - d) * 60).toInt()
        var s = (v - d - m / 60.0) * 3600
        val rounded = String.format(Locale.ROOT, "%.${secDecimals}f", s).toDouble()
        if (rounded >= 60.0) { s = 0.0; m += 1 } else s = rounded
        if (m >= 60) { m -= 60; d += 1 }
        return String.format(Locale.ROOT, "%d°%02d'%0${3 + secDecimals}.${secDecimals}f\"%s", d, m, s, hemi)
    }

    /**
     * Lit « 32°53'14.2"N », « N 32 53 14.2 », « -32.887 », « 32,887 ». Retourne null si illisible.
     */
    fun parse(text: String): Double? {
        var t = text.trim().uppercase(Locale.ROOT).replace(',', '.')
        if (t.isEmpty()) return null
        var sign = 1.0
        if (t.startsWith("-")) { sign = -1.0; t = t.substring(1) }
        else if (t.startsWith("+")) t = t.substring(1)
        val letter = t.firstOrNull { it in "NSEW" }
        if (letter != null) {
            if (letter == 'S' || letter == 'W') sign = -sign
            t = t.replace(letter.toString(), " ")
        }
        val parts = Regex("[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+").findAll(t).map { it.value.toDouble() }.toList()
        if (parts.isEmpty() || parts.size > 3) return null
        if (t.any { it.isLetter() }) return null
        val d = parts[0]; val m = parts.getOrElse(1) { 0.0 }; val s = parts.getOrElse(2) { 0.0 }
        if (m >= 60.0 || s >= 60.0) return null
        return sign * (d + m / 60.0 + s / 3600.0)
    }
}
