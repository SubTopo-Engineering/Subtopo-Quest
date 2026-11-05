import math
A_WGS=6378137.0; F_WGS=1/298.257223563
A_INT=6378388.0; F_INT=1/297.0
def ell(a,f): return dict(a=a,f=f,b=a*(1-f),e2=f*(2-f))
WGS84=ell(A_WGS,F_WGS); INTL=ell(A_INT,F_INT)
ED50_TO_WGS84=(-87.0,-98.0,-121.0)

def _coef(n):
    n2,n3,n4,n5,n6=n**2,n**3,n**4,n**5,n**6
    al=[n/2-2/3*n2+5/16*n3+41/180*n4-127/288*n5+7891/37800*n6,
        13/48*n2-3/5*n3+557/1440*n4+281/630*n5-1983433/1935360*n6,
        61/240*n3-103/140*n4+15061/26880*n5+167603/181440*n6,
        49561/161280*n4-179/168*n5+6601661/7257600*n6,
        34729/80640*n5-3418889/1995840*n6,
        212378941/319334400*n6]
    be=[n/2-2/3*n2+37/96*n3-1/360*n4-81/512*n5+96199/604800*n6,
        1/48*n2+1/15*n3-437/1440*n4+46/105*n5-1118711/3870720*n6,
        17/480*n3-37/840*n4-209/4480*n5+5569/90720*n6,
        4397/161280*n4-11/504*n5-830251/7257600*n6,
        4583/161280*n5-108847/3991680*n6,
        20648693/638668800*n6]
    de=[2*n-2/3*n2-2*n3+116/45*n4+26/45*n5-2854/675*n6,
        7/3*n2-8/5*n3-227/45*n4+2704/315*n5+2323/945*n6,
        56/15*n3-136/35*n4-1262/105*n5+73814/2835*n6,
        4279/630*n4-332/35*n5-399572/14175*n6,
        4174/315*n5-144838/6237*n6,
        601676/22275*n6]
    return al,be,de

K0=0.9996
def zone_of(lon): return min(60,int((lon+180)//6)+1)
def cm(zone): return -183+6*zone

def to_utm(lat,lon,zone=None,ellp=WGS84):
    zone=zone or zone_of(lon)
    a,f=ellp['a'],ellp['f']; n=f/(2-f)
    al,_,_=_coef(n); A=a/(1+n)*(1+n**2/4+n**4/64+n**6/256)
    e=2*math.sqrt(n)/(1+n)
    p=math.radians(lat); l=math.radians(lon-cm(zone))
    t=math.sinh(math.atanh(math.sin(p))-e*math.atanh(e*math.sin(p)))
    xi0=math.atan2(t,math.cos(l)); eta0=math.asinh(math.sin(l)/math.hypot(t,math.cos(l)))
    xi=xi0+sum(al[j-1]*math.sin(2*j*xi0)*math.cosh(2*j*eta0) for j in range(1,7))
    eta=eta0+sum(al[j-1]*math.cos(2*j*xi0)*math.sinh(2*j*eta0) for j in range(1,7))
    N0=0.0 if lat>=0 else 10_000_000.0
    return zone,(lat>=0),500000+K0*A*eta,N0+K0*A*xi

def from_utm(zone,north,E,N,ellp=WGS84):
    a,f=ellp['a'],ellp['f']; n=f/(2-f)
    _,be,de=_coef(n); A=a/(1+n)*(1+n**2/4+n**4/64+n**6/256)
    xi0=(N-(0 if north else 1e7))/(K0*A); eta0=(E-500000)/(K0*A)
    xi=xi0-sum(be[j-1]*math.sin(2*j*xi0)*math.cosh(2*j*eta0) for j in range(1,7))
    eta=eta0-sum(be[j-1]*math.cos(2*j*xi0)*math.sinh(2*j*eta0) for j in range(1,7))
    chi=math.asin(math.sin(xi)/math.cosh(eta))
    lat=chi+sum(de[j-1]*math.sin(2*j*chi) for j in range(1,7))
    lon=math.atan2(math.sinh(eta),math.cos(xi))
    return math.degrees(lat),math.degrees(lon)+cm(zone)

def to_ecef(lat,lon,h,e):
    p,l=math.radians(lat),math.radians(lon)
    N=e['a']/math.sqrt(1-e['e2']*math.sin(p)**2)
    return ((N+h)*math.cos(p)*math.cos(l),(N+h)*math.cos(p)*math.sin(l),(N*(1-e['e2'])+h)*math.sin(p))
def from_ecef(x,y,z,e):
    lon=math.atan2(y,x); p=math.hypot(x,y); lat=math.atan2(z,p*(1-e['e2']))
    for _ in range(10):
        N=e['a']/math.sqrt(1-e['e2']*math.sin(lat)**2)
        h=p/math.cos(lat)-N
        lat=math.atan2(z,p*(1-e['e2']*N/(N+h)))
    N=e['a']/math.sqrt(1-e['e2']*math.sin(lat)**2); h=p/math.cos(lat)-N
    return math.degrees(lat),math.degrees(lon),h
def ed50_to_wgs84(lat,lon,h=0.0,t=ED50_TO_WGS84):
    x,y,z=to_ecef(lat,lon,h,INTL); return from_ecef(x+t[0],y+t[1],z+t[2],WGS84)
def wgs84_to_ed50(lat,lon,h=0.0,t=ED50_TO_WGS84):
    x,y,z=to_ecef(lat,lon,h,WGS84); return from_ecef(x-t[0],y-t[1],z-t[2],INTL)

def vincenty_inverse(lat1,lon1,lat2,lon2,e=WGS84):
    a,f,b=e['a'],e['f'],e['b']
    if lat1==lat2 and lon1==lon2: return 0.0,0.0,0.0
    U1=math.atan((1-f)*math.tan(math.radians(lat1))); U2=math.atan((1-f)*math.tan(math.radians(lat2)))
    L=math.radians(lon2-lon1); lam=L
    sU1,cU1,sU2,cU2=math.sin(U1),math.cos(U1),math.sin(U2),math.cos(U2)
    for _ in range(200):
        sl,cl=math.sin(lam),math.cos(lam)
        ss=math.hypot(cU2*sl,cU1*sU2-sU1*cU2*cl)
        cs=sU1*sU2+cU1*cU2*cl; sig=math.atan2(ss,cs)
        sa=cU1*cU2*sl/ss; c2a=1-sa*sa
        c2sm=cs-2*sU1*sU2/c2a if c2a!=0 else 0.0
        C=f/16*c2a*(4+f*(4-3*c2a))
        lp=lam
        lam=L+(1-C)*f*sa*(sig+C*ss*(c2sm+C*cs*(-1+2*c2sm**2)))
        if abs(lam-lp)<1e-12: break
    else: return None
    u2=c2a*(a*a-b*b)/(b*b)
    A=1+u2/16384*(4096+u2*(-768+u2*(320-175*u2)))
    B=u2/1024*(256+u2*(-128+u2*(74-47*u2)))
    ds=B*ss*(c2sm+B/4*(cs*(-1+2*c2sm**2)-B/6*c2sm*(-3+4*ss**2)*(-3+4*c2sm**2)))
    s=b*A*(sig-ds)
    a1=math.atan2(cU2*math.sin(lam),cU1*sU2-sU1*cU2*math.cos(lam))
    a2=math.atan2(cU1*math.sin(lam),-sU1*cU2+cU1*sU2*math.cos(lam))
    return s,(math.degrees(a1)+360)%360,(math.degrees(a2)+360)%360

def vincenty_direct(lat1,lon1,az,s,e=WGS84):
    a,f,b=e['a'],e['f'],e['b']
    a1=math.radians(az); U1=math.atan((1-f)*math.tan(math.radians(lat1)))
    sU1,cU1=math.sin(U1),math.cos(U1)
    sig1=math.atan2(math.tan(U1),math.cos(a1)); sa=cU1*math.sin(a1); c2a=1-sa*sa
    u2=c2a*(a*a-b*b)/(b*b)
    A=1+u2/16384*(4096+u2*(-768+u2*(320-175*u2)))
    B=u2/1024*(256+u2*(-128+u2*(74-47*u2)))
    sig=s/(b*A)
    for _ in range(200):
        c2sm=math.cos(2*sig1+sig); ss,cs=math.sin(sig),math.cos(sig)
        ds=B*ss*(c2sm+B/4*(cs*(-1+2*c2sm**2)-B/6*c2sm*(-3+4*ss**2)*(-3+4*c2sm**2)))
        sp=sig; sig=s/(b*A)+ds
        if abs(sig-sp)<1e-12: break
    ss,cs=math.sin(sig),math.cos(sig); c2sm=math.cos(2*sig1+sig)
    x=sU1*ss-cU1*cs*math.cos(a1)
    p2=math.atan2(sU1*cs+cU1*ss*math.cos(a1),(1-f)*math.hypot(sa,x))
    lam=math.atan2(ss*math.sin(a1),cU1*cs-sU1*ss*math.cos(a1))
    C=f/16*c2a*(4+f*(4-3*c2a))
    L=lam-(1-C)*f*sa*(sig+C*ss*(c2sm+C*cs*(-1+2*c2sm**2)))
    return math.degrees(p2),lon1+math.degrees(L)

def haversine(lat1,lon1,lat2,lon2,R=6371008.8):
    p1,p2=math.radians(lat1),math.radians(lat2)
    h=math.sin((p2-p1)/2)**2+math.cos(p1)*math.cos(p2)*math.sin(math.radians(lon2-lon1)/2)**2
    return 2*R*math.asin(math.sqrt(h))

def shoelace(pts):
    s=0.0
    for i in range(len(pts)):
        x1,y1=pts[i]; x2,y2=pts[(i+1)%len(pts)]; s+=x1*y2-x2*y1
    return abs(s)/2
def perimeter_planar(pts): return sum(math.dist(pts[i],pts[(i+1)%len(pts)]) for i in range(len(pts)))

def _q(phi,e):
    es=math.sqrt(e['e2']); s=math.sin(phi)
    return (1-e['e2'])*(s/(1-e['e2']*s*s)-1/(2*es)*math.log((1-es*s)/(1+es*s)))
def ellipsoidal_area(pts,e=WGS84):
    """pts: [(lat,lon)]. Equal-area (authalic) cylindrical projection; edges straight in (lon,q)."""
    qp=_q(math.pi/2,e); Rq=e['a']*math.sqrt(qp/2)
    ys=[_q(math.radians(la),e)/qp for la,_ in pts]; ls=[math.radians(lo) for _,lo in pts]
    s=0.0
    for i in range(len(pts)):
        j=(i+1)%len(pts); dl=ls[j]-ls[i]
        dl=(dl+math.pi)%(2*math.pi)-math.pi
        s+=dl*(ys[i]+ys[j])/2
    return abs(s)*Rq*Rq
