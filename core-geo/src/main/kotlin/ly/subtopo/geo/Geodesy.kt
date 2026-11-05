package ly.subtopo.geo

import kotlin.math.*

/** Résultat géodésique : distance (m), azimut initial et final (degrés, 0-360 depuis le nord vrai). */
data class Inverse(val distance: Double, val azimuth1: Double, val azimuth2: Double)

object Geodesy {
    private const val MEAN_RADIUS = 6371008.8

    /** Distance sphérique rapide (erreur jusqu'à ~0,5 %). */
    fun haversine(a: LatLon, b: LatLon): Double {
        val p1 = Math.toRadians(a.lat); val p2 = Math.toRadians(b.lat)
        val h = sin((p2 - p1) / 2).pow(2) + cos(p1) * cos(p2) * sin(Math.toRadians(b.lon - a.lon) / 2).pow(2)
        return 2 * MEAN_RADIUS * asin(sqrt(h))
    }

    /** Problème géodésique inverse (Vincenty). Retourne null si non convergent (points quasi antipodaux). */
    fun inverse(p1: LatLon, p2: LatLon, e: Ellipsoid = Ellipsoid.WGS84): Inverse? {
        if (p1 == p2) return Inverse(0.0, 0.0, 0.0)
        val a = e.a; val f = e.f; val b = e.b
        val u1 = atan((1 - f) * tan(Math.toRadians(p1.lat)))
        val u2 = atan((1 - f) * tan(Math.toRadians(p2.lat)))
        val bigL = Math.toRadians(p2.lon - p1.lon)
        var lam = bigL
        val sU1 = sin(u1); val cU1 = cos(u1); val sU2 = sin(u2); val cU2 = cos(u2)
        var ss = 0.0; var cs = 0.0; var sig = 0.0; var c2a = 0.0; var c2sm = 0.0
        var converged = false
        for (i in 0 until 200) {
            val sl = sin(lam); val cl = cos(lam)
            ss = hypot(cU2 * sl, cU1 * sU2 - sU1 * cU2 * cl)
            cs = sU1 * sU2 + cU1 * cU2 * cl
            sig = atan2(ss, cs)
            val sa = cU1 * cU2 * sl / ss
            c2a = 1 - sa * sa
            c2sm = if (c2a != 0.0) cs - 2 * sU1 * sU2 / c2a else 0.0
            val c = f / 16 * c2a * (4 + f * (4 - 3 * c2a))
            val prev = lam
            lam = bigL + (1 - c) * f * sa * (sig + c * ss * (c2sm + c * cs * (-1 + 2 * c2sm * c2sm)))
            if (abs(lam - prev) < 1e-12) { converged = true; break }
        }
        if (!converged) return null
        val uSq = c2a * (a * a - b * b) / (b * b)
        val bigA = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
        val bigB = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
        val ds = bigB * ss * (c2sm + bigB / 4 * (cs * (-1 + 2 * c2sm * c2sm) -
                bigB / 6 * c2sm * (-3 + 4 * ss * ss) * (-3 + 4 * c2sm * c2sm)))
        val s = b * bigA * (sig - ds)
        val a1 = atan2(cU2 * sin(lam), cU1 * sU2 - sU1 * cU2 * cos(lam))
        val a2 = atan2(cU1 * sin(lam), -sU1 * cU2 + cU1 * sU2 * cos(lam))
        return Inverse(s, norm360(Math.toDegrees(a1)), norm360(Math.toDegrees(a2)))
    }

    /** Problème géodésique direct (Vincenty) : point atteint depuis [p] avec [azimuthDeg] et [distance] (m). */
    fun direct(p: LatLon, azimuthDeg: Double, distance: Double, e: Ellipsoid = Ellipsoid.WGS84): LatLon {
        val a = e.a; val f = e.f; val b = e.b
        val a1 = Math.toRadians(azimuthDeg)
        val u1 = atan((1 - f) * tan(Math.toRadians(p.lat)))
        val sU1 = sin(u1); val cU1 = cos(u1)
        val sig1 = atan2(tan(u1), cos(a1))
        val sa = cU1 * sin(a1)
        val c2a = 1 - sa * sa
        val uSq = c2a * (a * a - b * b) / (b * b)
        val bigA = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
        val bigB = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
        var sig = distance / (b * bigA)
        for (i in 0 until 200) {
            val c2sm = cos(2 * sig1 + sig); val ss = sin(sig); val cs = cos(sig)
            val ds = bigB * ss * (c2sm + bigB / 4 * (cs * (-1 + 2 * c2sm * c2sm) -
                    bigB / 6 * c2sm * (-3 + 4 * ss * ss) * (-3 + 4 * c2sm * c2sm)))
            val prev = sig
            sig = distance / (b * bigA) + ds
            if (abs(sig - prev) < 1e-12) break
        }
        val ss = sin(sig); val cs = cos(sig); val c2sm = cos(2 * sig1 + sig)
        val x = sU1 * ss - cU1 * cs * cos(a1)
        val phi2 = atan2(sU1 * cs + cU1 * ss * cos(a1), (1 - f) * hypot(sa, x))
        val lam = atan2(ss * sin(a1), cU1 * cs - sU1 * ss * cos(a1))
        val c = f / 16 * c2a * (4 + f * (4 - 3 * c2a))
        val l = lam - (1 - c) * f * sa * (sig + c * ss * (c2sm + c * cs * (-1 + 2 * c2sm * c2sm)))
        var lon = p.lon + Math.toDegrees(l)
        if (lon > 180.0) lon -= 360.0
        if (lon < -180.0) lon += 360.0
        return LatLon(Math.toDegrees(phi2), lon)
    }

    /** Distance ellipsoïdale (Vincenty, repli Haversine) en mètres. */
    fun distance(p1: LatLon, p2: LatLon): Double = inverse(p1, p2)?.distance ?: haversine(p1, p2)

    /** Longueur d'une polyligne géographique (m). */
    fun pathLength(points: List<LatLon>): Double =
        points.zipWithNext().sumOf { (a, b) -> distance(a, b) }

    internal fun norm360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0
}
