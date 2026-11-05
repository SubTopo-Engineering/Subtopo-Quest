package ly.subtopo.geo

import kotlin.math.*

/** Coordonnées UTM. [north] = hémisphère nord (false = sud, faux nord 10 000 000 m). */
data class UtmCoord(val zone: Int, val north: Boolean, val easting: Double, val northing: Double) {
    override fun toString() = "%d%s %.2f %.2f".format(java.util.Locale.ROOT, zone, if (north) "N" else "S", easting, northing)
}

/**
 * Projection UTM (Transverse Mercator, k0 = 0,9996), série de Krüger d'ordre 6 (Karney 2011).
 * Précision sub-millimétrique à l'intérieur de la zone et au-delà de ~±10°.
 * Zones 1 à 60, hémisphères N et S. Une zone peut être imposée (utile en Libye : zones 32 à 34).
 */
object Utm {
    const val K0 = 0.9996
    private const val FALSE_EASTING = 500_000.0
    private const val FALSE_NORTHING_SOUTH = 10_000_000.0

    fun zoneOf(lon: Double): Int = min(60, floor((lon + 180.0) / 6.0).toInt() + 1)
    fun centralMeridian(zone: Int): Double = -183.0 + 6.0 * zone

    private class Coef(n: Double) {
        val alpha: DoubleArray; val beta: DoubleArray; val delta: DoubleArray
        init {
            val n2 = n * n; val n3 = n2 * n; val n4 = n3 * n; val n5 = n4 * n; val n6 = n5 * n
            alpha = doubleArrayOf(
                n / 2 - 2.0 / 3 * n2 + 5.0 / 16 * n3 + 41.0 / 180 * n4 - 127.0 / 288 * n5 + 7891.0 / 37800 * n6,
                13.0 / 48 * n2 - 3.0 / 5 * n3 + 557.0 / 1440 * n4 + 281.0 / 630 * n5 - 1983433.0 / 1935360 * n6,
                61.0 / 240 * n3 - 103.0 / 140 * n4 + 15061.0 / 26880 * n5 + 167603.0 / 181440 * n6,
                49561.0 / 161280 * n4 - 179.0 / 168 * n5 + 6601661.0 / 7257600 * n6,
                34729.0 / 80640 * n5 - 3418889.0 / 1995840 * n6,
                212378941.0 / 319334400 * n6)
            beta = doubleArrayOf(
                n / 2 - 2.0 / 3 * n2 + 37.0 / 96 * n3 - 1.0 / 360 * n4 - 81.0 / 512 * n5 + 96199.0 / 604800 * n6,
                1.0 / 48 * n2 + 1.0 / 15 * n3 - 437.0 / 1440 * n4 + 46.0 / 105 * n5 - 1118711.0 / 3870720 * n6,
                17.0 / 480 * n3 - 37.0 / 840 * n4 - 209.0 / 4480 * n5 + 5569.0 / 90720 * n6,
                4397.0 / 161280 * n4 - 11.0 / 504 * n5 - 830251.0 / 7257600 * n6,
                4583.0 / 161280 * n5 - 108847.0 / 3991680 * n6,
                20648693.0 / 638668800 * n6)
            delta = doubleArrayOf(
                2 * n - 2.0 / 3 * n2 - 2 * n3 + 116.0 / 45 * n4 + 26.0 / 45 * n5 - 2854.0 / 675 * n6,
                7.0 / 3 * n2 - 8.0 / 5 * n3 - 227.0 / 45 * n4 + 2704.0 / 315 * n5 + 2323.0 / 945 * n6,
                56.0 / 15 * n3 - 136.0 / 35 * n4 - 1262.0 / 105 * n5 + 73814.0 / 2835 * n6,
                4279.0 / 630 * n4 - 332.0 / 35 * n5 - 399572.0 / 14175 * n6,
                4174.0 / 315 * n5 - 144838.0 / 6237 * n6,
                601676.0 / 22275 * n6)
        }
    }

    private fun rectifyingRadius(n: Double, a: Double) =
        a / (1 + n) * (1 + n * n / 4 + n.pow(4) / 64 + n.pow(6) / 256)

    /** Géographique -> UTM. [zone] null = zone naturelle de la longitude. */
    fun fromLatLon(p: LatLon, zone: Int? = null, ellipsoid: Ellipsoid = Ellipsoid.WGS84): UtmCoord {
        val z = zone ?: zoneOf(p.lon)
        require(z in 1..60) { "Zone UTM invalide : $z" }
        val n = ellipsoid.f / (2 - ellipsoid.f)
        val c = Coef(n)
        val bigA = rectifyingRadius(n, ellipsoid.a)
        val e = 2 * sqrt(n) / (1 + n)
        val phi = Math.toRadians(p.lat)
        val lam = Math.toRadians(p.lon - centralMeridian(z))
        val t = sinh(atanh(sin(phi)) - e * atanh(e * sin(phi)))
        val xi0 = atan2(t, cos(lam))
        val eta0 = asinh(sin(lam) / hypot(t, cos(lam)))
        var xi = xi0; var eta = eta0
        for (j in 1..6) {
            xi += c.alpha[j - 1] * sin(2 * j * xi0) * cosh(2 * j * eta0)
            eta += c.alpha[j - 1] * cos(2 * j * xi0) * sinh(2 * j * eta0)
        }
        val north = p.lat >= 0
        return UtmCoord(z, north,
            FALSE_EASTING + K0 * bigA * eta,
            (if (north) 0.0 else FALSE_NORTHING_SOUTH) + K0 * bigA * xi)
    }

    /** UTM -> géographique. */
    fun toLatLon(u: UtmCoord, ellipsoid: Ellipsoid = Ellipsoid.WGS84): LatLon {
        require(u.zone in 1..60) { "Zone UTM invalide : ${u.zone}" }
        val n = ellipsoid.f / (2 - ellipsoid.f)
        val c = Coef(n)
        val bigA = rectifyingRadius(n, ellipsoid.a)
        val xi0 = (u.northing - (if (u.north) 0.0 else FALSE_NORTHING_SOUTH)) / (K0 * bigA)
        val eta0 = (u.easting - FALSE_EASTING) / (K0 * bigA)
        var xi = xi0; var eta = eta0
        for (j in 1..6) {
            xi -= c.beta[j - 1] * sin(2 * j * xi0) * cosh(2 * j * eta0)
            eta -= c.beta[j - 1] * cos(2 * j * xi0) * sinh(2 * j * eta0)
        }
        val chi = asin(sin(xi) / cosh(eta))
        var lat = chi
        for (j in 1..6) lat += c.delta[j - 1] * sin(2 * j * chi)
        val lon = atan2(sinh(eta), cos(xi))
        var lonDeg = Math.toDegrees(lon) + centralMeridian(u.zone)
        if (lonDeg > 180.0) lonDeg -= 360.0
        if (lonDeg < -180.0) lonDeg += 360.0
        return LatLon(Math.toDegrees(lat), lonDeg)
    }
}
