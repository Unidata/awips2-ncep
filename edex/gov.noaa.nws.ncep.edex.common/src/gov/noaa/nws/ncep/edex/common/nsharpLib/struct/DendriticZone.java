package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;
/**
 * 
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store dendritic layers parameters.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 09/1/2017   RM#34794    Chin Chen   Initial coding for 
 *                                      NSHARP - Updates for March 2017 bigSharp version
 *                                      - Update the dendritic growth layer calculations and other skewT
 *                                      updates.
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 */

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;

public class DendriticZone {
	private float bottomPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    private float topPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    
    // zone bottom height msl in ft
    private float bottomHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // zone top height msl in ft
    private float topHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    public DendriticZone() {
        super();
        this.bottomPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.topPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.bottomHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.topHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

	public float getBottomPress() {
		return bottomPress;
	}

	public void setBottomPress(float bottomPress) {
		this.bottomPress = bottomPress;
	}

	public float getTopPress() {
		return topPress;
	}

	public void setTopPress(float topPress) {
		this.topPress = topPress;
	}

	public float getBottomHeight() {
		return bottomHeight;
	}

	public void setBottomHeight(float bottomHeight) {
		this.bottomHeight = bottomHeight;
	}

	public float getTopHeight() {
		return topHeight;
	}

	public void setTopHeight(float topHeight) {
		this.topHeight = topHeight;
	}
}
