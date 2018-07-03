package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store return parameters for 
 * lapse_rate_max().
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 06/13/2017   RM#34793    Chin Chen   created for storing return parameters for 
 * lapse_rate_max()
 * 
 * </pre>
 * 
 * @author Chin Chen 
 * @version 1.0
 * 
 */
public class LapseRateMax {
    private float lrm; //lapse rate max in  C/km    
    private int color; //gempak color
    private float lrmHeight; //height of lrm layer in m AGL
    
    
    public LapseRateMax() {
        super();
        this.lrm = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.lrmHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.color = 0;
    }
    public float getLrm() {
        return lrm;
    }
    public void setLrm(float lrm) {
        this.lrm = lrm;
    }
    public int getColor() {
        return color;
    }
    public void setColor(int color) {
        this.color = color;
    }
    public float getLrmHeight() {
        return lrmHeight;
    }
    public void setLrmHeight(float lrmHeight) {
        this.lrmHeight = lrmHeight;
    }
    
}
