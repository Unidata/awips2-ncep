package gov.noaa.nws.ncep.common.dataplugin.ncgrib.request;

import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Request to store a grid record.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/01/2016   R6821      kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
@DynamicSerialize
public class StoreGridRequest implements IServerRequest {

    @DynamicSerializeElement
    private GridRecord gridRecord;

    public StoreGridRequest() {
    }

    public StoreGridRequest(GridRecord gridRecord) {
        this.gridRecord = gridRecord;
    }

    /**
     * @return the gridRecord
     */
    public GridRecord getGridRecord() {
        return gridRecord;
    }

    /**
     * @param gridRecord
     *            the gridRecord to set
     */
    public void setGridRecord(GridRecord gridRecord) {
        this.gridRecord = gridRecord;
    }
}
