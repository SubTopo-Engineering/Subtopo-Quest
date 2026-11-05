package ly.subtopo.geo

import kotlin.math.*

/** Paramètres de translation géocentrique (m) du datum local vers WGS84 : WGS84 = local + (dx, dy, dz). */
data class Shift3(val dx: Double, val dy: Double, val dz: Double)

/**
 * Transformation ED50 <-> WGS84 par translation géocentrique à 3 paramètres.
 *
 * ATTENTION : [DEFAULT_ED50_TO_WGS84] (-87, -98, -121) est la valeur « Europe occidentale » (EPSG:1133,
 * précision ~3 m). Elle n'est PAS un paramètre officiel pour la Libye. Les géomètres doivent fournir ou
 * valider les paramètres propres à leur zone / organisme, puis les passer au constructeur.
 */
class Ed50Transform(private val shift: Shift3 = DEFAULT_ED50_TO_WGS84) {
    private val intl = Ellipsoid.INTERNATIONAL_1924
    private val wgs = Ellipsoid.WGS84

    fun toWgs84(lat: Double, lon: Double, h: Double = 0.0): Triple<Double, Double, Double> {
        val (x, y, z) = toEcef(lat, lon, h, intl)
        return fromEcef(x + shift.dx, y + shift.dy, z + shift.dz, wgs)
    }

    fun fromWgs84(lat: Double, lon: Double, h: Double = 0.0): Triple<Double, Double, Double> {
        val (x, y, z) = toEcef(lat, lon, h, wgs)
        return fromEcef(x - shift.dx, y - shift.dy, z - shift.dz, intl)
    }

    /** Position ED50 (lat, lon) -> WGS84. */
    fun toWgs84(p: LatLon): LatLon = toWgs84(p.lat, p.lon).let { LatLon(it.first, it.second) }
    /** Position WGS84 -> ED50. */
    fun fromWgs84(p: LatLon): LatLon = fromWgs84(p.lat, p.lon).let { LatLon(it.first, it.second) }

    /** UTM ED50 (ellipsoïde International 1924) -> WGS84 géographique. */
    fun utmEd50ToWgs84(u: UtmCoord): LatLon = toWgs84(Utm.toLatLon(u, intl))
    /** WGS84 géographique -> UTM ED50. */
    fun wgs84ToUtmEd50(p: LatLon, zone: Int? = null): UtmCoord = Utm.fromLatLon(fromWgs84(p), zone, intl)

    private fun toEcef(lat: Double, lon: Double, h: Double, e: Ellipsoid): Triple<Double, Double, Double> {
        val p = Math.toRadians(lat); val l = Math.toRadians(lon)
        val n = e.a / sqrt(1 - e.e2 * sin(p).pow(2))
        return Triple((n + h) * cos(p) * cos(l), (n + h) * cos(p) * sin(l), (n * (1 - e.e2) + h) * sin(p))
    }

    private fun fromEcef(x: Double, y: Double, z: Double, e: Ellipsoid): Triple<Double, Double, Double> {
        val lon = atan2(y, x)
        val p = hypot(x, y)
        var lat = atan2(z, p * (1 - e.e2))
        repeat(10) {
            val n = e.a / sqrt(1 - e.e2 * sin(lat).pow(2))
            val h = p / cos(lat) - n
            lat = atan2(z, p * (1 - e.e2 * n / (n + h)))
        }
        val n = e.a / sqrt(1 - e.e2 * sin(lat).pow(2))
        return Triple(Math.toDegrees(lat), Math.toDegrees(lon), p / cos(lat) - n)
    }

    companion object {
        val DEFAULT_ED50_TO_WGS84 = Shift3(-87.0, -98.0, -121.0)
    }
}
