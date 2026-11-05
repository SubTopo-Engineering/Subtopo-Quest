import math, random
from geo_ref import *
ok=0
def check(name,cond,detail=""):
    global ok
    print(("PASS " if cond else "FAIL ")+name+("  "+detail if detail else ""))
    ok+= 0 if cond else 1

# 1. Point de référence : équateur / méridien central
z,n,E,N=to_utm(0,3); check("UTM (0,3) -> 500000,0",abs(E-500000)<1e-6 and abs(N)<1e-6,f"E={E:.6f} N={N:.6f}")
# 2. Arc de méridien à 45° (4 984 944.378 m) * k0
z,n,E,N=to_utm(45,3); exp=0.9996*4984944.378
check("UTM (45,3) N = k0*arc(45°)",abs(N-exp)<0.01,f"N={N:.4f} attendu={exp:.4f}")
# 3. Hémisphère sud : faux nord
z,n,E,N=to_utm(-45,3); check("Hémisphère S : N = 10 000 000 - 4 982 950.4",abs(N-(1e7-exp))<0.01,f"N={N:.3f}")
# 4. Numérotation des zones
check("zone_of Tripoli 13.19E = 33",zone_of(13.19)==33)
check("zone_of -180 = 1, 179.99 = 60, 180 = 60",zone_of(-180)==1 and zone_of(179.99)==60 and zone_of(180)==60)
# 5. Aller-retour, 60 zones x 2 hémisphères
random.seed(1); worst=0
for zn in range(1,61):
    for _ in range(300):
        lat=random.uniform(-80,84); lon=cm(zn)+random.uniform(-3.5,3.5)
        z,nn,E,N=to_utm(lat,lon,zn); la,lo=from_utm(z,nn,E,N)
        worst=max(worst,abs(la-lat),abs(lo-lon))
check("Aller-retour UTM (18 000 pts, 60 zones)",worst<1e-9,f"écart max={worst:.2e}° (~{worst*111000*1000:.3f} mm)")
# 6. Comparaison indépendante avec la série de Snyder (Redfearn) à ~2° du méridien central
def snyder(lat,lon,zone):
    a=A_WGS;e2=WGS84['e2'];ep2=e2/(1-e2);p=math.radians(lat);dl=math.radians(lon-cm(zone))
    N=a/math.sqrt(1-e2*math.sin(p)**2);T=math.tan(p)**2;C=ep2*math.cos(p)**2;Aa=dl*math.cos(p)
    M=a*((1-e2/4-3*e2**2/64-5*e2**3/256)*p-(3*e2/8+3*e2**2/32+45*e2**3/1024)*math.sin(2*p)+(15*e2**2/256+45*e2**3/1024)*math.sin(4*p)-(35*e2**3/3072)*math.sin(6*p))
    E=500000+K0*N*(Aa+(1-T+C)*Aa**3/6+(5-18*T+T*T+72*C-58*ep2)*Aa**5/120)
    Nn=K0*(M+N*math.tan(p)*(Aa**2/2+(5-T+9*C+4*C*C)*Aa**4/24+(61-58*T+T*T+600*C-330*ep2)*Aa**6/720))
    return E,Nn
worst=0
for lat,lon in [(32.8872,13.1913),(30.0,11.0),(25.0,20.0),(0.5,17.9),(55,10.5)]:
    zn=zone_of(lon);_,_,E,N=to_utm(lat,lon,zn);E2,N2=snyder(lat,lon,zn);worst=max(worst,abs(E-E2),abs(N-N2))
check("Concordance avec formule de Snyder (≤ 2 cm)",worst<0.02,f"écart max={worst*1000:.2f} mm")
# 7. Zone forcée (Libye : 9°E–25°E couvre zones 32 à 34) : le point reste calculable hors zone
_,_,E,N=to_utm(32.9,19.0,33); la,lo=from_utm(33,True,E,N)
check("Zone forcée (point à 4° du MC, aller-retour)",abs(la-32.9)<1e-8 and abs(lo-19.0)<1e-8,f"E={E:.1f}")
# 8. ED50 <-> WGS84
worst=0
for lat,lon in [(32.8872,13.1913),(32.1,20.1),(27,14),(48.85,2.35)]:
    la,lo,h=ed50_to_wgs84(lat,lon,0); la2,lo2,h2=wgs84_to_ed50(la,lo,h)
    worst=max(worst,abs(la2-lat),abs(lo2-lon),abs(h2))
check("Aller-retour ED50<->WGS84",worst<1e-9,f"écart max={worst:.2e}")
la,lo,_=ed50_to_wgs84(32.8872,13.1913); d,_,_=vincenty_inverse(32.8872,13.1913,la,lo)
check("Décalage ED50->WGS84 à Tripoli plausible (100–250 m)",100<d<250,f"{d:.1f} m")
# 9. Vincenty : exemple Geoscience Australia (Flinders Peak -> Buninyong)
def dms(d,m,s,sg=1): return sg*(d+m/60+s/3600)
la1,lo1=dms(37,57,3.72030,-1),dms(144,25,29.52440); la2,lo2=dms(37,39,10.15610,-1),dms(143,55,35.38390)
s,a1,a2=vincenty_inverse(la1,lo1,la2,lo2)
check("Vincenty distance 54 972.271 m",abs(s-54972.271)<0.005,f"s={s:.4f}")
check("Vincenty azimut 306°52'05.37\"",abs(a1-dms(306,52,5.37))<2e-6,f"az={a1:.7f}°")
# 10. Vincenty direct inverse de l'inverse
la,lo=vincenty_direct(la1,lo1,a1,s); check("Vincenty direct = inverse",abs(la-la2)<1e-9 and abs(lo-lo2)<1e-9,f"{abs(la-la2):.1e} {abs(lo-lo2):.1e}")
# 11. Vincenty vs Haversine (<0.6 %)
s,_,_=vincenty_inverse(32.8872,13.1913,32.1,20.1); h=haversine(32.8872,13.1913,32.1,20.1)
check("Vincenty ≈ Haversine (<0.6 %)",abs(s-h)/s<0.006,f"{s/1000:.3f} km vs {h/1000:.3f} km")
check("Points identiques -> 0",vincenty_inverse(10,10,10,10)[0]==0)
# 12. Aire : carré UTM 1 km x 1 km
sq=[(0,0),(1000,0),(1000,1000),(0,1000)]
check("Shoelace carré 1 km = 1 000 000 m²",shoelace(sq)==1e6); check("Périmètre = 4000 m",perimeter_planar(sq)==4000)
sq_utm=[(500000-500+x,4000000+y) for x,y in sq]; pts=[from_utm(33,True,E,N) for E,N in sq_utm]
A=ellipsoidal_area(pts); exp=1e6/K0**2
check("Aire ellipsoïdale carré 1 km (≈ 1e6/k0²)",abs(A-exp)<1.0,f"{A:.2f} m² attendu≈{exp:.2f}")
# 13. Aire : cellule 1°x1° vs intégration numérique indépendante  dA = M N cos(phi) dphi dlam
cell=[(10,10),(10,11),(11,11),(11,10)]; A=ellipsoidal_area(cell)
e2=WGS84['e2'];a=A_WGS;num=0;K=20000
for i in range(K):
    p=math.radians(10+(i+.5)/K); w=math.sqrt(1-e2*math.sin(p)**2)
    num+=a*a*(1-e2)*math.cos(p)/w**4*math.radians(1)/K*math.radians(1)
check("Aire cellule 1°x1° vs intégration numérique",abs(A-num)/num<1e-6,f"{A/1e6:.4f} km² vs {num/1e6:.4f} km²")
print("\nÉCHECS:",ok)
