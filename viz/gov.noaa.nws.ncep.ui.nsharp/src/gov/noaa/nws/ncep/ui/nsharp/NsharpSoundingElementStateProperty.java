package gov.noaa.nws.ncep.ui.nsharp;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.skewt.rsc.NsharpSoundingElementStateProperty
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Apr 23, 2012  229      Chin Chen  Initial coding
 * Jan 27, 2015  17006    Chin Chen  NSHARP freezes when loading a sounding from
 *                                   MDCRS products in Volume Browser
 * Dec 14, 2018  6872     bsteffen   Remove unused fields.
 * 
 * </pre>
 * 
 * @author Chin Chen
 */

public class NsharpSoundingElementStateProperty {
    private String elementDescription;

    private NsharpStationInfo stnInfo;

    private int compColorIndex;

    private List<NcSoundingLayer> sndLyLst;

    private List<NcSoundingLayer> sndLyLstBk;

    private boolean goodData = true;

    public NsharpSoundingElementStateProperty(String elementDescription,
            NsharpStationInfo stnInfo, List<NcSoundingLayer> sndLyLst,
            boolean goodData) {
        super();
        this.elementDescription = elementDescription;
        this.stnInfo = stnInfo;
        this.compColorIndex = NsharpConstants.LINE_COMP1;
        this.sndLyLst = sndLyLst;
        this.goodData = goodData;
        sndLyLstBk = new ArrayList<>(sndLyLst.size());
        for (NcSoundingLayer ly : sndLyLst) {
            try {
                sndLyLstBk.add((NcSoundingLayer) ly.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getElementDescription() {
        return elementDescription;
    }

    public NsharpStationInfo getStnInfo() {
        return stnInfo;
    }

    public int getCompColorIndex() {
        return compColorIndex;
    }

    public void setCompColorIndex(int compColorIndex) {
        this.compColorIndex = compColorIndex;
    }

    public List<NcSoundingLayer> getSndLyLst() {
        return sndLyLst;
    }

    public List<NcSoundingLayer> getSndLyLstBk() {
        return sndLyLstBk;
    }

    public void setSndLyLstBk(List<NcSoundingLayer> sndLyLstBk) {
        this.sndLyLstBk = sndLyLstBk;
    }

    public void restoreSndLyLstFromBackup() {
        sndLyLst.clear();
        for (NcSoundingLayer ly : sndLyLstBk) {
            try {
                sndLyLst.add((NcSoundingLayer) ly.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isGoodData() {
        return goodData;
    }

    public void setGoodData(boolean goodData) {
        this.goodData = goodData;
    }

    @Override
    public String toString() {
        if (isGoodData()) {
            return elementDescription;
        } else {
            return elementDescription + ": INVALID";
        }
    }

}