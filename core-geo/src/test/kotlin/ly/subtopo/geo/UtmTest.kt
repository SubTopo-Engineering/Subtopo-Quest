package ly.subtopo.geo

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.*

class UtmTest {
    @Test fun equatorCentralMeridian() {
        val u = Utm.fromLatLon(LatLon(0.0, 3.0))
        assertEquals(31, u.zone); assertEquals(500000.0, u.easting, 1e-6); assertEquals(0.0, u.northing, 1e-6)
    }
    @Test fun meridianArc45() {
        val u = Utm.fromLatLon(LatLon(45.0, 3.0))
        assertEquals(0.9996 * 4984944.378, u.northing, 0.01)
    }
    @Test fun southernHemisphereFalseNorthing() {
        val u = Utm.fromLatLon(LatLon(-45.0, 3.0))
        assertFalse(u.north); assertEquals(1e7 - 0.9996 * 4984944.378, u.northing, 0.01)
    }
    @Test fun zones() {
        assertEquals(33, Utm.zoneOf(13.19)); assertEquals(1, Utm.zoneOf(-180.0))
        assertEquals(60, Utm.zoneOf(179.99)); assertEquals(60, Utm.zoneOf(180.0))
    }
    @Test fun roundTripAll60Zones() {
        val rnd = Random(1); var worst = 0.0
        for (z in 1..60) repeat(300) {
            val p = LatLon(rnd.nextDouble(-80.0, 84.0), Utm.centralMeridian(z) + rnd.nextDouble(-3.5, 3.5))
            val back = Utm.toLatLon(Utm.fromLatLon(p, z))
            worst = max(worst, max(abs(back.lat - p.lat), abs(back.lon - p.lon)))
        }
        assertTrue(worst < 1e-9, "écart max = $worst")
    }
    @Test fun forcedZoneRoundTrip() {
        val back = Utm.toLatLon(Utm.fromLatLon(LatLon(32.9, 19.0), 33))
        assertEquals(32.9, back.lat, 1e-8); assertEquals(19.0, back.lon, 1e-8)
    }
}

class Ed50Test {
    private val t = Ed50Transform()
    @Test fun roundTrip() {
        for (p in listOf(LatLon(32.8872, 13.1913), LatLon(32.1, 20.1), LatLon(27.0, 14.0))) {
            val back = t.fromWgs84(t.toWgs84(p))
            assertEquals(p.lat, back.lat, 1e-8); assertEquals(p.lon, back.lon, 1e-8)
        }
    }
    @Test fun shiftMagnitudeTripoli() {
        val p = LatLon(32.8872, 13.1913)
        val d = Geodesy.distance(p, t.toWgs84(p))
        assertTrue(d in 100.0..250.0, "décalage = $d m")   // valeur Python de référence : 147,5 m
    }
    @Test fun utmEd50RoundTrip() {
        val p = LatLon(32.8872, 13.1913)
        val back = t.utmEd50ToWgs84(t.wgs84ToUtmEd50(p, 33))
        assertEquals(p.lat, back.lat, 1e-8); assertEquals(p.lon, back.lon, 1e-8)
    }
}

class GeodesyTest {
    private fun dms(d: Int, m: Int, s: Double, sign: Int = 1) = sign * (d + m / 60.0 + s / 3600.0)
    private val flinders = LatLon(dms(37, 57, 3.72030, -1), dms(144, 25, 29.52440))
    private val buninyong = LatLon(dms(37, 39, 10.15610, -1), dms(143, 55, 35.38390))

    @Test fun vincentyGeoscienceAustraliaExample() {
        val r = assertNotNull(Geodesy.inverse(flinders, buninyong))
        assertEquals(54972.271, r.distance, 0.005)
        assertEquals(dms(306, 52, 5.37), r.azimuth1, 2e-6)
    }
    @Test fun directIsInverseOfInverse() {
        val r = assertNotNull(Geodesy.inverse(flinders, buninyong))
        val p = Geodesy.direct(flinders, r.azimuth1, r.distance)
        assertEquals(buninyong.lat, p.lat, 1e-9); assertEquals(buninyong.lon, p.lon, 1e-9)
    }
    @Test fun vincentyVsHaversine() {
        val a = LatLon(32.8872, 13.1913); val b = LatLon(32.1, 20.1)
        val s = Geodesy.distance(a, b)
        assertTrue(abs(s - Geodesy.haversine(a, b)) / s < 0.006)
    }
    @Test fun samePointIsZero() = assertEquals(0.0, Geodesy.distance(LatLon(10.0, 10.0), LatLon(10.0, 10.0)))
}

class SurveyingTest {
    @Test fun squareAreaAndPerimeter() {
        val sq = listOf(PlanPoint(0.0, 0.0), PlanPoint(1000.0, 0.0), PlanPoint(1000.0, 1000.0), PlanPoint(0.0, 1000.0))
        assertEquals(1e6, Surveying.planArea(sq), 1e-6); assertEquals(4000.0, Surveying.planPerimeter(sq), 1e-9)
    }
    @Test fun ellipsoidalAreaOfUtmSquare() {
        val corners = listOf(0.0 to 0.0, 1000.0 to 0.0, 1000.0 to 1000.0, 0.0 to 1000.0)
            .map { (x, y) -> Utm.toLatLon(UtmCoord(33, true, 499500.0 + x, 4000000.0 + y)) }
        assertEquals(1e6 / (0.9996 * 0.9996), Surveying.ellipsoidalArea(corners), 1.0)
    }
    @Test fun ellipsoidalAreaOneDegreeCell() {
        val a = Surveying.ellipsoidalArea(listOf(LatLon(10.0, 10.0), LatLon(10.0, 11.0), LatLon(11.0, 11.0), LatLon(11.0, 10.0)))
        assertEquals(12108.1886e6, a, 12108.1886e6 * 1e-6)  // intégration numérique indépendante (Python)
    }
    @Test fun gisementAndPolar() {
        val a = PlanPoint(1000.0, 1000.0)
        assertEquals(0.0, Surveying.gisementDeg(a, PlanPoint(1000.0, 1100.0)), 1e-9)
        assertEquals(90.0, Surveying.gisementDeg(a, PlanPoint(1100.0, 1000.0)), 1e-9)
        assertEquals(225.0, Surveying.gisementDeg(a, PlanPoint(900.0, 900.0)), 1e-9)
        val p = Surveying.polar(a, 90.0, 50.0); assertEquals(1050.0, p.e, 1e-9); assertEquals(1000.0, p.n, 1e-9)
        assertEquals(100.0, Surveying.degToGon(90.0), 1e-12)
    }
    @Test fun gainLoss() {
        val (g, l) = Surveying.cumulativeGainLoss(listOf(100.0, 110.0, 105.0, 120.0))
        assertEquals(25.0, g, 1e-9); assertEquals(5.0, l, 1e-9)
    }
}

class CoordFormatTest {
    @Test fun dmsFormat() = assertEquals("32°53'14.00\"N", CoordFormat.dms(32.0 + 53 / 60.0 + 14 / 3600.0, true))
    @Test fun dmsSouthWest() = assertEquals("13°11'30.00\"W", CoordFormat.dms(-(13.0 + 11 / 60.0 + 30 / 3600.0), false))
    @Test fun parseVariants() {
        val v = 32.0 + 53 / 60.0 + 14.2 / 3600.0
        assertEquals(v, CoordFormat.parse("32°53'14.2\"N")!!, 1e-9)
        assertEquals(v, CoordFormat.parse("N 32 53 14.2")!!, 1e-9)
        assertEquals(-32.887, CoordFormat.parse("-32,887")!!, 1e-9)
        assertEquals(-13.5, CoordFormat.parse("13 30 0 W")!!, 1e-9)
        assertNull(CoordFormat.parse("abc")); assertNull(CoordFormat.parse("32 75 0"))
    }
}
