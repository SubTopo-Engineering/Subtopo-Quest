package ly.subtopo.geo

import kotlin.math.sqrt

/** Ellipsoïde de référence. */
data class Ellipsoid(val name: String, val a: Double, val invF: Double) {
    val f: Double get() = 1.0 / invF
    val b: Double get() = a * (1.0 - f)
    val e2: Double get() = f * (2.0 - f)
    val ep2: Double get() = e2 / (1.0 - e2)

    companion object {
        val WGS84 = Ellipsoid("WGS84", 6378137.0, 298.257223563)
        /** Hayford / International 1924, ellipsoïde de ED50. */
        val INTERNATIONAL_1924 = Ellipsoid("International 1924", 6378388.0, 297.0)
    }
}

/** Position géographique en degrés décimaux. */
data class LatLon(val lat: Double, val lon: Double) {
    init {
        require(lat in -90.0..90.0) { "Latitude hors limites : $lat" }
        require(lon in -180.0..180.0) { "Longitude hors limites : $lon" }
    }
}
