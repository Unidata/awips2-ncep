package gov.noaa.nws.ncep.viz.resources;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;

/**
 * DfltRecordRscDataObj is part of the Resource classes redesign as specified
 * in Redmine 11819.  For more details about that redesign please see that
 * ticket and the title block comment of 
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * Before the redesign DfltRecordRscDataObj was an inner class of
 * AbstractNatlCntrsResource. 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/05/2015   R 11819    srussell     Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class DfltRecordRscDataObj implements IRscDataObject {
    private PluginDataObject pdo;

    public DfltRecordRscDataObj(PluginDataObject o) {
        pdo = o;
    }

    @Override
    public DataTime getDataTime() {
        return pdo.getDataTime();
    }

    public PluginDataObject getPDO() {
        return pdo;
    }
}