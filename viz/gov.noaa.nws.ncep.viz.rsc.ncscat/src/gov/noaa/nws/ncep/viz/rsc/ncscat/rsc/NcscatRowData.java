package gov.noaa.nws.ncep.viz.rsc.ncscat.rsc;

import gov.noaa.nws.ncep.viz.resources.IRscDataObject;

import java.util.ArrayList;
import java.util.Calendar;

import com.raytheon.uf.common.time.DataTime;

/**
 * NcscatRowData - Class to hold numerical and boolean data for a single
 * (cross-track) row of wind vector cells, across the swath width of the
 * satellite instrument.
 * 
 * By the time the data are stored here, they have been normalized somewhat,
 * mainly to remove instrument-specific idiosyncrasies (like whether the
 * "direction" of wind is "from" or "to") so the resource can handle things
 * consistently. Note that no drawable graphical objects are stored here.
 * 
 * Across all instrument types, each row has a time stamp marking the
 * observation time, and a list of individual point observations. (Points are
 * not time-stamped individually.)
 * 
 * 
 * This code has been developed by NCEP for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/27/2016   R10155     B. Hebbard  Spun off from NcscatResource 
 *                                     (was formerly an inner class)
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
class NcscatRowData implements IRscDataObject {
    Calendar rowTime; // timestamp on this row

    ArrayList<NcscatPointData> rowPoints; // individual points in row

    public NcscatRowData(Calendar rowTime) {
        this.rowTime = rowTime;
        this.rowPoints = new ArrayList<NcscatPointData>();
    }

    @Override
    public DataTime getDataTime() {
        if (rowTime == null) {
            return null;
        } else {
            return new DataTime(rowTime);
        }
    }
}