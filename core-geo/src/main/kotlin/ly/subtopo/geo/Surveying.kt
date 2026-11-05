package ly.subtopo.geo

import kotlin.math.*

/** Point plan (E, N) en mètres, par ex. coordonnées UTM d'une même zone. */
data class PlanPoint(val e: Double, val n: Double)

/** Calculs topographiques. Angles en degrés décimaux, 0-360, sens horaire depuis le nord. */
object Surveying {
    /** Gisement (angle du grille, depuis le nord de la grille) du point A vers B. Pas de conversion de zone. */
    fun gisementDeg(a: PlanPoint, b: PlanPoint): Double =
        Geodesy.norm360(Math.toDegrees(atan2(b.e - a.e, b.n - a.n)))

    /** Conversion degrés -> grades (gon) : 400 gon = 360°. */
    fun degToGon(deg: Double) = deg * 400.0 / 360.0
    fun gonToDeg(gon: Double) = gon * 360.0 / 400.0

    fun planDistance(a: PlanPoint, b: PlanPoint) = hypot(b.e - a.e, b.n - a.n)

    /** Différence de coordonnées (dE, dN) de A vers B. */
    fun deltas(a: PlanPoint, b: PlanPoint) = Pair(b.e - a.e, b.n - a.n)

    /** Point obtenu depuis A avec un gisement (degrés) et une distance horizontale (m). */
    fun polar(a: PlanPoint, gisementDeg: Double, distance: Double): PlanPoint {
        val g = Math.toRadians(gisementDeg)
        return PlanPoint(a.e + distance * sin(g), a.n + distance * cos(g))
    }

    /** Dénivelé (m) = altitude finale - altitude initiale. */
    fun elevationDifference(zA: Double, zB: Double) = zB - zA

    /** Pente en % entre deux points (distance horizontale > 0). */
    fun slopePercent(horizontalDistance: Double, dz: Double): Double {
        require(horizontalDistance > 0) { "Distance horizontale nulle" }
        return dz / horizontalDistance * 100.0
    }

    /** Dénivelé positif / négatif cumulé d'un profil d'altitudes, avec seuil anti-bruit (m). */
    fun cumulativeGainLoss(altitudes: List<Double>, threshold: Double = 0.0): Pair<Double, Double> {
        var gain = 0.0; var loss = 0.0
        if (altitudes.isEmpty()) return Pair(0.0, 0.0)
        var ref = altitudes[0]
        for (z in altitudes.drop(1)) {
            val d = z - ref
            if (d >= threshold && d > 0) { gain += d; ref = z }
            else if (-d >= threshold && d < 0) { loss += -d; ref = z }
        }
        return Pair(gain, loss)
    }

    /** Surface (m²) d'un polygone plan, formule du lacet (indépendant du sens). */
    fun planArea(pts: List<PlanPoint>): Double {
        require(pts.size >= 3) { "Un polygone nécessite au moins 3 sommets" }
        var s = 0.0
        for (i in pts.indices) {
            val a = pts[i]; val b = pts[(i + 1) % pts.size]
            s += a.e * b.n - b.e * a.n
        }
        return abs(s) / 2.0
    }

    /** Périmètre (m) d'un polygone plan fermé. */
    fun planPerimeter(pts: List<PlanPoint>): Double =
        pts.indices.sumOf { planDistance(pts[it], pts[(it + 1) % pts.size]) }

    /**
     * Surface ellipsoïdale (m²) d'un polygone géographique, par projection équivalente (authalique).
     * C'est la surface « terrain » au niveau de l'ellipsoïde. La surface UTM ([planArea]) diffère
     * d'environ k² (≈ 0,08 % au méridien central) : les deux sont à présenter distinctement.
     */
    fun ellipsoidalArea(pts: List<LatLon>, e: Ellipsoid = Ellipsoid.WGS84): Double {
        require(pts.size >= 3) { "Un polygone nécessite au moins 3 sommets" }
        val qp = q(PI / 2, e)
        val rq = e.a * sqrt(qp / 2)
        val ys = pts.map { q(Math.toRadians(it.lat), e) / qp }
        val ls = pts.map { Math.toRadians(it.lon) }
        var s = 0.0
        for (i in pts.indices) {
            val j = (i + 1) % pts.size
            var dl = ls[j] - ls[i]
            dl = ((dl + PI) % (2 * PI) + 2 * PI) % (2 * PI) - PI
            s += dl * (ys[i] + ys[j]) / 2
        }
        return abs(s) * rq * rq
    }

    private fun q(phi: Double, e: Ellipsoid): Double {
        val es = sqrt(e.e2); val s = sin(phi)
        return (1 - e.e2) * (s / (1 - e.e2 * s * s) - 1 / (2 * es) * ln((1 - es * s) / (1 + es * s)))
    }
}
