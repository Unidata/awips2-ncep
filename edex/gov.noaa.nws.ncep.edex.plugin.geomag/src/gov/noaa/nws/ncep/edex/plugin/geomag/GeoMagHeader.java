/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.edex.plugin.geomag;

import com.raytheon.uf.common.time.DataTime;

/**
 * Class that represents a geomag data header
 * 
 * <pre>
 * * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer        Description
 * ------------ ---------- -----------     --------------------------
 * 10/07/2015   R11429     sgurung,jtravis Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GeoMagHeader {

    private int sourceId = 101;

    private String stationCode = null;

    private String unit = null;

    private DataTime headerTime = null;

    /**
     * @return the sourceId
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId
     *            the sourceId to set
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the stationCode
     */
    public String getStationCode() {
        return stationCode;
    }

    /**
     * @param stationCode
     *            the stationCode to set
     */
    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit
     *            the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the headerTime
     */
    public DataTime getHeaderTime() {
        return headerTime;
    }

    /**
     * @param headerTime
     *            the headerTime to set
     */
    public void setHeaderTime(DataTime headerTime) {
        this.headerTime = headerTime;
    }

    public boolean isUnitsAssigned() {

        boolean assigned = false;

        if (this.unit != null) {
            assigned = true;
        }

        return assigned;

    }

}
