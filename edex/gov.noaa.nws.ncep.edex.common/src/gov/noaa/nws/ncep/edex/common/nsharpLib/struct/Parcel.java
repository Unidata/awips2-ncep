package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store parcel parameters.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 08/24/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement. 
 * 
 * </pre>
 * 
 * @author Chin Chen 
 * @version 1.0
 * 
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;

public class Parcel {
    private float lplpres;

    private float lpltemp;

    private float lpldwpt;

    private float blayer;

    private float tlayer;

    private float lclpres;

    private float lfcpres;

    private float elpres;

    private float mplpres;

    private float bplus;

    private float bminus;

    private float bfzl;

    private float cape3km;

    private float cape6km;

    private float wm10c;

    private float wm20c;

    private float wm30c;

    private float li5;

    private float li3;

    private float brn;

    private float brnShear;

    private float limax;

    private float limaxpres;

    private float cap;

    private float cappres;

    private float mixRatio;

    private int parcelNumber;

    // LCL AGL in ft
    private float lclAgl;

    // LFC AGL in ft
    private float lfcAgl;

    // EL AGL in ft
    private float elAgl;

    // MPL AGL in ft
    private float mplAgl;

    // LFC temerature in C
    private float lfcTemp;

    // ELtemerature in C
    private float elTemp;

    public Parcel() {
        super();
        this.lplpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lpltemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lpldwpt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.blayer = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.tlayer = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lclpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lclAgl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lfcpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lfcAgl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lfcTemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.elpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.elAgl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.elTemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mplpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mplAgl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.bplus = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.bminus = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.bfzl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.cape3km = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.cape6km = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.wm10c = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.wm20c = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.wm30c = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.li5 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.li3 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.brn = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.brnShear = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.limax = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.limaxpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.cap = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.cappres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mixRatio = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public float getLplpres() {
        return lplpres;
    }

    public void setLplpres(float lplpres) {
        this.lplpres = lplpres;
    }

    public float getLpltemp() {
        return lpltemp;
    }

    public void setLpltemp(float lpltemp) {
        this.lpltemp = lpltemp;
    }

    public float getLpldwpt() {
        return lpldwpt;
    }

    public void setLpldwpt(float lpldwpt) {
        this.lpldwpt = lpldwpt;
    }

    public float getBlayer() {
        return blayer;
    }

    public void setBlayer(float blayer) {
        this.blayer = blayer;
    }

    public float getTlayer() {
        return tlayer;
    }

    public void setTlayer(float tlayer) {
        this.tlayer = tlayer;
    }

    public float getLclpres() {
        return lclpres;
    }

    public void setLclpres(float lclpres) {
        this.lclpres = lclpres;
    }

    public float getLfcpres() {
        return lfcpres;
    }

    public void setLfcpres(float lfcpres) {
        this.lfcpres = lfcpres;
    }

    public float getElpres() {
        return elpres;
    }

    public void setElpres(float elpres) {
        this.elpres = elpres;
    }

    public float getMplpres() {
        return mplpres;
    }

    public void setMplpres(float mplpres) {
        this.mplpres = mplpres;
    }

    public float getBplus() {
        return bplus;
    }

    public void setBplus(float bplus) {
        this.bplus = bplus;
    }

    public float getBminus() {
        return bminus;
    }

    public void setBminus(float bminus) {
        this.bminus = bminus;
    }

    public float getBfzl() {
        return bfzl;
    }

    public void setBfzl(float bfzl) {
        this.bfzl = bfzl;
    }

    public float getCape3km() {
        return cape3km;
    }

    public void setCape3km(float cape3km) {
        this.cape3km = cape3km;
    }

    public float getCape6km() {
        return cape6km;
    }

    public void setCape6km(float cape6km) {
        this.cape6km = cape6km;
    }

    public float getWm10c() {
        return wm10c;
    }

    public void setWm10c(float wm10c) {
        this.wm10c = wm10c;
    }

    public float getWm20c() {
        return wm20c;
    }

    public void setWm20c(float wm20c) {
        this.wm20c = wm20c;
    }

    public float getWm30c() {
        return wm30c;
    }

    public void setWm30c(float wm30c) {
        this.wm30c = wm30c;
    }

    public float getLi5() {
        return li5;
    }

    public void setLi5(float li5) {
        this.li5 = li5;
    }

    public float getLi3() {
        return li3;
    }

    public void setLi3(float li3) {
        this.li3 = li3;
    }

    public float getBrn() {
        return brn;
    }

    public void setBrn(float brn) {
        this.brn = brn;
    }

    public float getBrnShear() {
        return brnShear;
    }

    public void setBrnShear(float brnShear) {
        this.brnShear = brnShear;
    }

    public float getLimax() {
        return limax;
    }

    public void setLimax(float limax) {
        this.limax = limax;
    }

    public float getLimaxpres() {
        return limaxpres;
    }

    public void setLimaxpres(float limaxpres) {
        this.limaxpres = limaxpres;
    }

    public float getCap() {
        return cap;
    }

    public void setCap(float cap) {
        this.cap = cap;
    }

    public float getCappres() {
        return cappres;
    }

    public void setCappres(float cappres) {
        this.cappres = cappres;
    }

    public int getParcelNumber() {
        return parcelNumber;
    }

    public void setParcelNumber(int parcelNumber) {
        this.parcelNumber = parcelNumber;
    }

    public float getMixRatio() {
        return mixRatio;
    }

    public void setMixRatio(float mixRatio) {
        this.mixRatio = mixRatio;
    }

    public float getLclAgl() {
        return lclAgl;
    }

    public void setLclAgl(float lclAgl) {
        this.lclAgl = lclAgl;
    }

    public float getLfcAgl() {
        return lfcAgl;
    }

    public void setLfcAgl(float lfcAgl) {
        this.lfcAgl = lfcAgl;
    }

    public float getElAgl() {
        return elAgl;
    }

    public void setElAgl(float elAgl) {
        this.elAgl = elAgl;
    }

    public float getMplAgl() {
        return mplAgl;
    }

    public void setMplAgl(float mplAgl) {
        this.mplAgl = mplAgl;
    }

    public float getLfcTemp() {
        return lfcTemp;
    }

    public void setLfcTemp(float lfcTemp) {
        this.lfcTemp = lfcTemp;
    }

    public float getElTemp() {
        return elTemp;
    }

    public void setElTemp(float elTemp) {
        this.elTemp = elTemp;
    }

}
